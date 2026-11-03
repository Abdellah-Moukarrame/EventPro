package org.example.eventpro.repository;

import jakarta.persistence.LockModeType;
import org.example.eventpro.entity.CategorieBilletEvenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface CategorieBilletEvenementRepository extends JpaRepository<CategorieBilletEvenement,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CategorieBilletEvenement> findWithLockById(Long id);
}
