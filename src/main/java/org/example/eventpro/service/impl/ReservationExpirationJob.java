package org.example.eventpro.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventpro.repository.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.Clock;
import static org.example.eventpro.enums.StatutReservation.PENDING_PAYMENT;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "reservation.expiration.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpirationJob {
    private final ReservationRepository reservations;
    private final ReservationLifecycle lifecycle;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${reservation.expiration.check-ms:60000}")
    public void expireReservations() {
        for (Long id : reservations.findExpiredIds(PENDING_PAYMENT, clock.instant())) {
            try {
                lifecycle.expire(id);
            } catch (RuntimeException ex) {
                log.warn("Expiration de la réservation {} à réessayer", id, ex);
            }
        }
    }
}
