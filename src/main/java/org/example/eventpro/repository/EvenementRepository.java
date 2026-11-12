package org.example.eventpro.repository;

import org.example.eventpro.entity.Evenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface EvenementRepository
        extends JpaRepository<Evenement, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Evenement> findWithLockById(Long id);
}
