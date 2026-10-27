
package org.example.eventpro.service;

import org.example.eventpro.dto.request.UtilisateurUpdateRequest;
import org.example.eventpro.dto.response.UtilisateurResponseDTO;

import java.util.List;

public interface UtilisateurService {


    List<UtilisateurResponseDTO> findAll();


    UtilisateurResponseDTO findById(Long id);


    UtilisateurResponseDTO update(
            Long id,
            UtilisateurUpdateRequest request
    );


    void delete(Long id);
}
