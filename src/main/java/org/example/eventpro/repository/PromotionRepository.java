package org.example.eventpro.repository;

import org.example.eventpro.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Promotion> findWithLockById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Promotion> findWithLockByEvenementIdAndCode(Long evenementId, String code);
    Optional<Promotion>findByEvenementIdAndCode(Long evenementId, String Code);
    boolean existsByEvenementIdAndCode(Long evenementId, String Code);
    List<Promotion> findByEvenementId(Long evenementId);
}
