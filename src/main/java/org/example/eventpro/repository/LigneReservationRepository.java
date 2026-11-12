package org.example.eventpro.repository;

import org.example.eventpro.entity.LigneReservation;
import org.example.eventpro.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;

public interface LigneReservationRepository extends JpaRepository<LigneReservation, Long> {
    boolean existsByCategorieBilletEvenementId(Long categorieId);

    @Query("""
        select coalesce(sum(l.quantite), 0) from LigneReservation l
        where l.categorieBilletEvenement.id = :categorieId
        and (:clientId is null or l.reservation.client.id = :clientId)
        and (l.reservation.statut = :confirmed or
            (l.reservation.statut = :pending and l.reservation.dateExpiration > :now))
        """)
    long quantiteOccupee(Long categorieId, Long clientId, Instant now,
                        StatutReservation confirmed, StatutReservation pending);
}
