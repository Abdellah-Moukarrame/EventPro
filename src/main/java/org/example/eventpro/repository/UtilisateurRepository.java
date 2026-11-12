
package org.example.eventpro.repository;

import org.example.eventpro.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    @Query("select u from Utilisateur u where type(u) = :typeUtilisateur order by u.id")
    List<Utilisateur> findByTypeUtilisateur(Class<? extends Utilisateur> typeUtilisateur);

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
