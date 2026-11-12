package org.example.eventpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.eventpro.entity.Reservation;
import org.example.eventpro.exceptions.BusinessException;
import org.example.eventpro.exceptions.ResourceNotFoundException;
import org.example.eventpro.repository.PromotionRepository;
import org.example.eventpro.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.time.Clock;
import static org.example.eventpro.enums.StatutReservation.*;

@Service
@RequiredArgsConstructor
public class ReservationLifecycle {
    private final ReservationRepository reservations;
    private final PromotionRepository promotions;
    private final Clock clock;

    // Expiration commits independently, even when a subsequent request fails.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expire(Long id) {
        reservations.findWithLockById(id).ifPresent(r -> {
            if (r.getStatut() == PENDING_PAYMENT && !r.getDateExpiration().isAfter(clock.instant())) {
                r.setStatut(EXPIRED);
                refreshPromotion(r);
            }
        });
    }

    @Transactional
    public void cancel(Long id) {
        Reservation r = reservations.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + id));
        if (r.getStatut() == CANCELLED || r.getStatut() == EXPIRED) return;
        if (r.getStatut() != PENDING_PAYMENT) {
            throw new BusinessException("Seule une réservation en attente de paiement peut être annulée");
        }
        r.setStatut(r.getDateExpiration().isAfter(clock.instant()) ? CANCELLED : EXPIRED);
        refreshPromotion(r);
    }

    private void refreshPromotion(Reservation r) {
        if (r.getPromotion() == null) return;
        var promotion = promotions.findWithLockById(r.getPromotion().getId()).orElseThrow();
        reservations.flush();
        promotion.setNombreUtilisations(Math.toIntExact(reservations.countActivePromotion(
                promotion.getId(), clock.instant(), CONFIRMED, PENDING_PAYMENT)));
    }
}
