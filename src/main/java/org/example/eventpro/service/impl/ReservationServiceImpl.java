package org.example.eventpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.eventpro.dto.request.*;
import org.example.eventpro.dto.response.ReservationResponseDTO;
import org.example.eventpro.entity.*;
import org.example.eventpro.enums.*;
import org.example.eventpro.exceptions.*;
import org.example.eventpro.mapper.ReservationMapper;
import org.example.eventpro.repository.*;
import org.example.eventpro.service.ReservationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import static org.example.eventpro.enums.StatutReservation.*;

@Service
@Validated
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
    private final ReservationRepository reservations;
    private final ClientRepository clients;
    private final CategorieBilletEvenementRepository categories;
    private final LigneReservationRepository lignes;
    private final PromotionRepository promotions;
    private final ReservationMapper mapper;
    private final ReservationLifecycle lifecycle;
    private final Clock clock;

    @Value("${reservation.payment-timeout:PT15M}")
    private Duration paymentTimeout;

    @Override
    @Transactional
    public ReservationResponseDTO create(ReservationCreateRequest request) {
        Client client = clients.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + request.clientId()));
        // Lock categories in a consistent order, including overlapping concurrent baskets.
        Map<Long, Integer> quantities = new TreeMap<>();
        for (var line : request.lignes()) {
            if (quantities.putIfAbsent(line.categorieBilletEvenementId(), line.quantite()) != null)
                throw new BusinessException("Une catégorie ne peut apparaître qu'une seule fois dans le panier");
        }
        List<CategorieBilletEvenement> locked = new ArrayList<>();
        for (Long id : quantities.keySet()) {
            locked.add(categories.findWithLockById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie d'événement introuvable : " + id)));
        }
        Instant now = clock.instant();
        if (paymentTimeout.isNegative() || paymentTimeout.isZero())
            throw new IllegalStateException("reservation.payment-timeout doit être positif");
        Reservation reservation = new Reservation();
        reservation.setReference("RES-" + UUID.randomUUID());
        reservation.setClient(client);
        reservation.setDateCreation(now);
        reservation.setDateExpiration(now.plus(paymentTimeout));
        reservation.setStatut(PENDING_PAYMENT);
        Long eventId = locked.get(0).getEvenement().getId();
        BigDecimal total = BigDecimal.ZERO;
        for (var category : locked) {
            Evenement event = category.getEvenement();
            if (!eventId.equals(event.getId()))
                throw new BusinessException("Une réservation doit concerner un seul événement");
            if (event.getStatut() != StatutEvenement.PUBLISHED
                    || now.isBefore(event.getDateOuvertureVente())
                    || !now.isBefore(event.getDateClotureVente())
                    || !now.isBefore(event.getDateDebut()))
                throw new BusinessException("Les ventes de cet événement ne sont pas ouvertes");
            Instant deadline = event.getDateClotureVente().isBefore(event.getDateDebut())
                    ? event.getDateClotureVente() : event.getDateDebut();
            if (deadline.isBefore(reservation.getDateExpiration())) reservation.setDateExpiration(deadline);
            int quantity = quantities.get(category.getId());
            long occupied = lignes.quantiteOccupee(category.getId(), null, now, CONFIRMED, PENDING_PAYMENT);
            if (quantity > (long) category.getQuantiteTotale() - occupied)
                throw new StockInsuffisantException("Stock insuffisant pour la catégorie " + category.getId());
            long clientQuantity = lignes.quantiteOccupee(category.getId(), client.getId(), now, CONFIRMED, PENDING_PAYMENT);
            if (clientQuantity + quantity > category.getQuotaMaxParClient())
                throw new QuotaDepasseException("Quota par client dépassé pour la catégorie " + category.getId());
            if (category.getPrixUnitaire().signum() < 0)
                throw new BusinessException("Le prix de la catégorie ne peut pas être négatif");
            LigneReservation line = new LigneReservation();
            line.setReservation(reservation);
            line.setCategorieBilletEvenement(category);
            line.setQuantite(quantity);
            line.setPrixUnitaire(category.getPrixUnitaire());
            line.setSousTotal(line.calculerSousTotal().setScale(2, RoundingMode.HALF_UP));
            checkAmount(line.getSousTotal());
            reservation.getLignesReservation().add(line);
            total = total.add(line.getSousTotal());
        }
        if (request.codePromotion() != null && !request.codePromotion().isBlank()) {
            Promotion promotion = promotions.findWithLockByEvenementIdAndCode(eventId, request.codePromotion().trim())
                    .orElseThrow(() -> new PromotionInvalideException("Code promotion invalide pour cet événement"));
            LocalDate today = LocalDate.now(clock);
            long uses = reservations.countActivePromotion(promotion.getId(), clock.instant(), CONFIRMED, PENDING_PAYMENT);
            if (today.isBefore(promotion.getDateDebut()) || today.isAfter(promotion.getDateFin())
                    || uses >= promotion.getLimiteUtilisation())
                throw new PromotionInvalideException("Promotion expirée, non commencée ou limite atteinte");
            BigDecimal value = promotion.getValeurReduction();
            if (value.signum() <= 0 || (promotion.getTypeReduction() == TypeReduction.PERCENTAGE
                    && value.compareTo(BigDecimal.valueOf(100)) > 0))
                throw new PromotionInvalideException("Valeur de réduction invalide");
            BigDecimal discount = promotion.getTypeReduction() == TypeReduction.PERCENTAGE
                    ? total.multiply(value).divide(BigDecimal.valueOf(100)) : value;
            total = total.subtract(discount).max(BigDecimal.ZERO);
            reservation.setPromotion(promotion);
            promotion.setNombreUtilisations(Math.toIntExact(uses + 1));
        }
        if (!reservation.getDateExpiration().isAfter(clock.instant()))
            throw new ReservationExpireeException("Le délai de réservation est déjà écoulé");
        checkAmount(total);
        reservation.setMontantTotal(total.setScale(2, RoundingMode.HALF_UP));
        return mapper.toDTO(reservations.saveAndFlush(reservation));
    }

    private void checkAmount(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new BusinessException("Montant trop élevé");
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponseDTO findById(Long id) {
        lifecycle.expire(id);
        return mapper.toDTO(reservations.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponseDTO findByReference(String reference) {
        Long id = reservations.findIdByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + reference));
        lifecycle.expire(id);
        return mapper.toDTO(reservations.findById(id).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> findByClientId(Long clientId) {
        if (!clients.existsById(clientId)) throw new ResourceNotFoundException("Client introuvable : " + clientId);
        reservations.findIdsByClientId(clientId).forEach(lifecycle::expire);
        return reservations.findByClientId(clientId).stream().map(mapper::toDTO).toList();
    }

    @Override
    public void cancel(Long id) { lifecycle.cancel(id); }
}
