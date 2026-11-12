package org.example.eventpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.eventpro.dto.response.BilletResponseDTO;
import org.example.eventpro.entity.Billet;
import org.example.eventpro.enums.*;
import org.example.eventpro.exceptions.*;
import org.example.eventpro.mapper.BilletMapper;
import org.example.eventpro.repository.*;
import org.example.eventpro.service.BilletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BilletServiceImpl implements BilletService {
    private final BilletRepository billets;
    private final ReservationRepository reservations;
    private final PaiementRepository paiements;
    private final BilletMapper mapper;
    private final Clock clock;

    @Override
    @Transactional
    public List<BilletResponseDTO> generateForReservation(Long id) {
        var reservation = reservations.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + id));
        if (reservation.getStatut() != StatutReservation.CONFIRMED
                || paiements.findByReservationId(id).filter(p -> p.getStatut() == StatutPaiement.SUCCESS).isEmpty())
            throw new BusinessException("Les billets nécessitent une réservation confirmée et un paiement réussi");
        if (reservation.getLignesReservation().isEmpty())
            throw new BusinessException("La réservation ne contient aucune ligne");
        LocalDateTime emission = LocalDateTime.now(clock);
        for (var ligne : reservation.getLignesReservation()) {
            long existing = billets.countByLigneReservationId(ligne.getId());
            if (ligne.getQuantite() < 1 || (existing != 0 && existing != ligne.getQuantite()))
                throw new BusinessException("Nombre de billets incohérent pour la ligne " + ligne.getId());
            if (existing == ligne.getQuantite()) continue;
            for (int i = 0; i < ligne.getQuantite(); i++) {
                billets.insertTicket("TKT-" + UUID.randomUUID(), emission, ligne.getId(),
                        ligne.getCategorieBilletEvenement().getCategorieBillet().getId());
            }
        }
        return list(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BilletResponseDTO findById(Long id) {
        return mapper.toDTO(billets.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Billet introuvable : " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public BilletResponseDTO findByCode(String code) { return mapper.toDTO(find(code)); }

    @Override
    @Transactional(readOnly = true)
    public List<BilletResponseDTO> findByReservationId(Long id) {
        if (!reservations.existsById(id)) throw new ResourceNotFoundException("Réservation introuvable : " + id);
        return list(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BilletResponseDTO verifier(String code, Long evenementId) {
        var billet = find(code);
        validate(billet, evenementId);
        return mapper.toDTO(billet);
    }

    @Override
    @Transactional
    public BilletResponseDTO controler(String code, Long evenementId) {
        var billet = billets.findWithLockByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Billet introuvable : " + code));
        validate(billet, evenementId);
        billet.setStatut(StatutBillet.USED);
        billet.setDateControle(LocalDateTime.now(clock));
        return mapper.toDTO(billets.saveAndFlush(billet));
    }

    private Billet find(String code) {
        return billets.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Billet introuvable : " + code));
    }

    private List<BilletResponseDTO> list(Long id) {
        return billets.findByLigneReservationReservationIdOrderByIdAsc(id).stream().map(mapper::toDTO).toList();
    }

    private void validate(Billet billet, Long evenementId) {
        if (billet.getStatut() == StatutBillet.USED) throw new BusinessException("Ce billet a déjà été utilisé");
        if (billet.getStatut() != StatutBillet.VALID) throw new BusinessException("Ce billet est annulé");
        var reservation = billet.getLigneReservation().getReservation();
        if (reservation.getStatut() != StatutReservation.CONFIRMED)
            throw new BusinessException("La réservation de ce billet n'est plus confirmée");
        var event = billet.getLigneReservation().getCategorieBilletEvenement().getEvenement();
        if (!event.getId().equals(evenementId))
            throw new BusinessException("Ce billet n'appartient pas à cet événement");
        if (event.getStatut() != StatutEvenement.PUBLISHED || !event.getDateFin().isAfter(clock.instant()))
            throw new BusinessException("Cet événement n'accepte plus de billets");
    }
}
