package org.example.eventpro.mapper;

import org.example.eventpro.entity.Billet;
import org.example.eventpro.dto.response.BilletResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BilletMapper {
    @Mapping(source = "ligneReservation.reservation.id", target = "reservationId")
    @Mapping(source = "ligneReservation.id", target = "ligneReservationId")
    @Mapping(source = "categorieBillet.id", target = "categorieBilletId")
    @Mapping(source = "ligneReservation.categorieBilletEvenement.evenement.id", target = "evenementId")
    BilletResponseDTO toDTO(Billet billet);
}
