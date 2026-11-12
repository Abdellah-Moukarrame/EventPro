package org.example.eventpro.repository;

import jakarta.persistence.LockModeType;
import org.example.eventpro.entity.Billet;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BilletRepository extends JpaRepository<Billet, Long> {
    Optional<Billet> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Billet> findWithLockByCode(String code);

    List<Billet> findByLigneReservationReservationIdOrderByIdAsc(Long reservationId);

    long countByLigneReservationId(Long ligneId);

    // Billet's constructor is protected. Insert through the repository without changing the entity.
    @Modifying(flushAutomatically = true)
    @Query(value = """
        insert into billets (code, date_emission, statut, ligne_reservation_id, categorie_billet_id)
        values (:code, :emission, 'VALID', :ligneId, :categorieId)
        """, nativeQuery = true)
    int insertTicket(String code, LocalDateTime emission, Long ligneId, Long categorieId);
}
