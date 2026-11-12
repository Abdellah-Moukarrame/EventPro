package org.example.eventpro.repository;

import org.example.eventpro.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    Optional<Paiement> findByReservationId(Long reservationId);
}
