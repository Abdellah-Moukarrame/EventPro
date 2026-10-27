package org.example.eventpro.repository;

import org.example.eventpro.entity.Organisateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisateurRepository
        extends JpaRepository<Organisateur, Long> {
}