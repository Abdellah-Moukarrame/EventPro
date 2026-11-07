
package org.example.eventpro.mapper;

import org.example.eventpro.dto.request.UtilisateurUpdateRequest;
import org.example.eventpro.dto.response.UtilisateurResponseDTO;
import org.example.eventpro.entity.Utilisateur;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UtilisateurMapper {


    UtilisateurResponseDTO toDTO(Utilisateur utilisateur);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "motDePasse", ignore = true)
    void updateEntity(
            UtilisateurUpdateRequest request,
            @MappingTarget Utilisateur utilisateur
    );
}
