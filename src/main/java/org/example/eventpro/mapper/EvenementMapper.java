package org.example.eventpro.mapper;

import org.example.eventpro.dto.request.EventCreateRequest;
import org.example.eventpro.dto.request.EventUpdateRequest;
import org.example.eventpro.dto.response.EventResponseDTO;
import org.example.eventpro.entity.Evenement;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EvenementMapper {

    @Mapping(target = "organisateurId", source = "organisateur.id")
    EventResponseDTO toDTO(Evenement evenement);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organisateur", ignore = true)
    @Mapping(target = "statut", ignore = true)
    Evenement toEntity(EventCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organisateur", ignore = true)
    @Mapping(target = "statut", ignore = true)
    void updateEntity(
            EventUpdateRequest request,
            @MappingTarget Evenement evenement
    );
}