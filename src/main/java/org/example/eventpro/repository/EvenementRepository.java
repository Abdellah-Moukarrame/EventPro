package org.example.eventpro.repository;

import org.example.eventpro.entity.Evenement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvenementRepository
        extends JpaRepository<Evenement, Long> {
}