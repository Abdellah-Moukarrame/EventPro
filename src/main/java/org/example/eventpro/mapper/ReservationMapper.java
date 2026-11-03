package org.example.eventpro.mapper;

import org.example.eventpro.dto.response.*;
import org.example.eventpro.entity.*;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "promotion.id", target = "promotionId")
    @Mapping(source = "lignesReservation", target = "lignes")
    ReservationResponseDTO toDTO(Reservation reservation);

    @Mapping(
            source = "categorieBilletEvenement.id",
            target = "categorieBilletEvenementId"
    )
    LigneReservationResponseDTO toDTO(LigneReservation ligne);
}