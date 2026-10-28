
package org.example.eventpro.service;

import org.example.eventpro.dto.request.PromotionCreateRequest;
import org.example.eventpro.dto.request.PromotionUpdateRequest;
import org.example.eventpro.dto.response.PromotionResponseDTO;

import java.util.List;

public interface PromotionService {

    PromotionResponseDTO create(PromotionCreateRequest request);

    PromotionResponseDTO update(
            Long id,
            PromotionUpdateRequest request
    );

    PromotionResponseDTO findById(Long id);

    List<PromotionResponseDTO> findByEvenementId(Long evenementId);

    void delete(Long id);
}
