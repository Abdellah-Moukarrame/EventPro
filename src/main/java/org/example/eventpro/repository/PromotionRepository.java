package org.example.eventpro.repository;

import org.example.eventpro.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion,Long> {
    Optional<Promotion>findByEvenementIdAndCode(Long evenementId, String Code);
    boolean existsByEvenementIdAndCode(Long evenementId, String Code);
    List<Promotion> findByEvenementId(Long evenementId);
}
