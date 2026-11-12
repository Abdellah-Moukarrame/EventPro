package org.example.eventpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.eventpro.dto.request.PaymentRequest;
import org.example.eventpro.dto.response.PaiementResponseDTO;
import org.example.eventpro.entity.Paiement;
import org.example.eventpro.exceptions.*;
import org.example.eventpro.repository.*;
import org.example.eventpro.service.PaiementService.PaymentResult;
import org.example.eventpro.service.BilletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.UUID;
import static org.example.eventpro.enums.StatutPaiement.*;
import static org.example.eventpro.enums.StatutReservation.*;

@Service
@RequiredArgsConstructor
public class PaiementTransaction {
    private final PaiementRepository paiements;
    private final ReservationRepository reservations;
    private final CategorieBilletEvenementRepository categories;
    private final Clock clock;
    private final BilletService billets;

    @Transactional
    public PaymentResult pay(PaymentRequest request) {
        // Same category lock order as reservation creation: never confirm stock already reallocated.
        for (Long categoryId : reservations.findCategoryIds(request.reservationId())) {
            categories.findWithLockById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + categoryId));
        }
        var reservation = reservations.findWithLockById(request.reservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + request.reservationId()));
        var existing = paiements.findByReservationId(reservation.getId());
        if (existing.isPresent() && existing.get().getStatut() != FAILED) {
            Paiement paiement = existing.get();
            if (paiement.getStatut() == SUCCESS && paiement.getMontant().compareTo(request.montant()) == 0) {
                return new PaymentResult(toDTO(paiement), false);
            }
            throw new BusinessException("Un paiement existe déjà pour cette réservation");
        }
        if (reservation.getStatut() != PENDING_PAYMENT)
            throw new BusinessException("La réservation doit être en attente de paiement");
        if (!reservation.getDateExpiration().isAfter(clock.instant()))
            throw new ReservationExpireeException("La réservation a expiré");
        if (request.montant().compareTo(reservation.getMontantTotal()) != 0)
            throw new BusinessException("Le montant doit correspondre exactement au total de la réservation");

        Paiement paiement = existing.orElseGet(() -> {
            Paiement nouveau = new Paiement();
            nouveau.setReference("PAY-" + UUID.randomUUID());
            nouveau.setIdempotencyKey(UUID.randomUUID().toString());
            return nouveau;
        });
        paiement.setMontant(reservation.getMontantTotal());
        paiement.setReservation(reservation);
        paiement.setStatut(SUCCESS);
        paiement.setDatePaiement(clock.instant());
        reservation.setStatut(CONFIRMED);
        paiements.saveAndFlush(paiement);
        billets.generateForReservation(reservation.getId());
        return new PaymentResult(toDTO(paiement), true);
    }


    @Transactional(readOnly = true)
    public PaiementResponseDTO findById(Long id) {
        return toDTO(paiements.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable : " + id)));
    }

    @Transactional(readOnly = true)
    public PaiementResponseDTO findByReservationId(Long id) {
        return toDTO(paiements.findByReservationId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable pour la réservation : " + id)));
    }

    private PaiementResponseDTO toDTO(Paiement p) {
        return new PaiementResponseDTO(p.getId(), p.getReference(),
                p.getReservation().getId(), p.getMontant(), p.getDatePaiement(), p.getStatut());
    }
}
