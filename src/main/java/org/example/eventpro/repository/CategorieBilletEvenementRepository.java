package org.example.eventpro.repository;

import jakarta.persistence.LockModeType;
import org.example.eventpro.entity.CategorieBilletEvenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

import java.util.Optional;

public interface CategorieBilletEvenementRepository extends JpaRepository<CategorieBilletEvenement,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CategorieBilletEvenement> findWithLockById(Long id);

    List<CategorieBilletEvenement> findByEvenementIdOrderByIdAsc(Long evenementId);
    boolean existsByEvenementIdAndCategorieBilletNomIgnoreCase(Long evenementId, String nom);

    @Query("select c.evenement.id from CategorieBilletEvenement c where c.id = :id")
    Optional<Long> findEvenementId(Long id);

    @Query("select coalesce(sum(c.quantiteTotale), 0) from CategorieBilletEvenement c where c.evenement.id = :evenementId")
    long quantiteConfiguree(Long evenementId);
}
