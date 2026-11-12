package org.example.eventpro.service;

import org.example.eventpro.dto.request.CategorieEvenementCreateRequest;
import org.example.eventpro.dto.request.CategorieEvenementUpdateRequest;
import org.example.eventpro.dto.response.CategorieEvenementResponseDTO;
import java.util.List;

public interface CategorieBilletEvenementService {
    CategorieEvenementResponseDTO create(CategorieEvenementCreateRequest request);
    List<CategorieEvenementResponseDTO> findAll(Long evenementId);
    CategorieEvenementResponseDTO findById(Long id);
    CategorieEvenementResponseDTO update(Long id, CategorieEvenementUpdateRequest request);
    void delete(Long id);
}
