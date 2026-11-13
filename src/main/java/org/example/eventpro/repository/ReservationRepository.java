
package org.example.eventpro.repository;

import org.example.eventpro.entity.Client;
import org.example.eventpro.entity.Reservation;
import org.example.eventpro.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reservation> findWithLockById(Long id);

    @Query("select l.categorieBilletEvenement.id from LigneReservation l where l.reservation.id = :id order by l.categorieBilletEvenement.id")
    List<Long> findCategoryIds(Long id);

    @Query("select r.id from Reservation r where r.reference = :reference")
    Optional<Long> findIdByReference(String reference);

    @Query("select r.id from Reservation r where r.client.id = :clientId order by r.id")
    List<Long> findIdsByClientId(Long clientId);

    @Query("select r.id from Reservation r where r.statut = :statut and r.dateExpiration <= :now order by r.id")
    List<Long> findExpiredIds(StatutReservation statut, Instant now);

    @Query("""
        select count(r) from Reservation r where r.promotion.id = :promotionId
        and (r.statut = :confirmed or (r.statut = :pending and r.dateExpiration > :now))
        """)
    long countActivePromotion(Long promotionId, Instant now,
                              StatutReservation confirmed, StatutReservation pending);

    boolean existsByReference(String reference);

    List<Reservation> findByClientId(Long clientId);

    List<Reservation> findByStatut(StatutReservation statut);

    List<Reservation> findByStatutAndDateExpirationBefore(
            StatutReservation statut,
            Instant date
    );

    @Query("""
            select r.client from Reservation r group by r.client order by count(r.client) desc limit 2 
            """)
    List<Client> findTwoMostReservedClient() ;

    // Mise en situation :

    @Query("""
           select r from Reservation r join r.clients cl 
           join r.ligne_reservation li 
           join r.categorie ca 
           where r.status =:CONFIRMED  
           and cl.email = :email 
           and ca.nom = :nomCategorie 
           and li.quantite >= :quantiteMin
           
            """)
    List<Reservation> findConfirmedReservationWithClientEmail(
            @Param("email") String email,
            @Param("nomCategorie") String nomCategorie,
            @Param("quantiteMin") int quantiteMin
    );


}
