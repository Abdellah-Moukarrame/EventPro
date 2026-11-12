package org.example.eventpro.mapper;

import org.example.eventpro.dto.response.CategorieEvenementResponseDTO;
import org.example.eventpro.entity.CategorieBilletEvenement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategorieEvenementMapper {
    @Mapping(source = "evenement.id", target = "evenementId")
    @Mapping(source = "categorieBillet.id", target = "categorieBilletId")
    @Mapping(source = "categorieBillet.nom", target = "nom")
    CategorieEvenementResponseDTO toDTO(CategorieBilletEvenement categorie);
}
