package org.example.eventpro.repository;

import org.example.eventpro.entity.CategorieBillet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TicketCategoryRepository extends JpaRepository<CategorieBillet, Long> {
    Optional<CategorieBillet> findFirstByNomIgnoreCaseOrderByIdAsc(String nom);
}
