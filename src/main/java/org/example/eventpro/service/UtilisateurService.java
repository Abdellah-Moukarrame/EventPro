
package org.example.eventpro.service;

import org.example.eventpro.dto.request.UtilisateurUpdateRequest;
import org.example.eventpro.dto.response.UtilisateurResponseDTO;

import java.util.List;

public interface UtilisateurService {

    // Récupérer tous les utilisateurs
    List<UtilisateurResponseDTO> findAll();

    // Récupérer un utilisateur par son ID
    UtilisateurResponseDTO findById(Long id);

    // Modifier les informations d'un utilisateur
    UtilisateurResponseDTO update(
            Long id,
            UtilisateurUpdateRequest request
    );

    // Supprimer un utilisateur
    void delete(Long id);
}
