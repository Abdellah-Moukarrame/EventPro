
package org.example.eventpro.mapper;

import org.example.eventpro.dto.request.PromotionCreateRequest;
import org.example.eventpro.dto.request.PromotionUpdateRequest;
import org.example.eventpro.dto.response.PromotionResponseDTO;
import org.example.eventpro.entity.Promotion;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PromotionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "evenement", ignore = true)
    @Mapping(target = "nombreUtilisations", ignore = true)
    Promotion toEntity(PromotionCreateRequest request);

    @Mapping(source = "evenement.id", target = "evenementId")
    PromotionResponseDTO toDTO(Promotion promotion);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "evenement", ignore = true)
    @Mapping(target = "nombreUtilisations", ignore = true)
    void updateEntity(
            PromotionUpdateRequest request,
            @MappingTarget Promotion promotion
    );
}
