
package org.example.eventpro.repository;

import org.example.eventpro.entity.Reservation;
import org.example.eventpro.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReference(String reference);

    boolean existsByReference(String reference);

    List<Reservation> findByClientId(Long clientId);

    List<Reservation> findByStatut(StatutReservation statut);

    List<Reservation> findByStatutAndDateExpirationBefore(
            StatutReservation statut,
            Instant date
    );
}
