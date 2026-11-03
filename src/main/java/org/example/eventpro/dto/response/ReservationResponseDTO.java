package org.example.eventpro.dto.response;

import org.example.eventpro.enums.StatutReservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReservationResponseDTO(
        Long id,
        String reference,
        Instant dateCreation,
        Instant dateExpiration,
        StatutReservation statut,
        BigDecimal montantTotal,
        Long clientId,
        Long promotionId,
        List<LigneReservationResponseDTO> lignes
) {}