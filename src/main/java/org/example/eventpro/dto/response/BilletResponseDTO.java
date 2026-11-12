package org.example.eventpro.dto.response;

import org.example.eventpro.enums.StatutBillet;
import java.time.LocalDateTime;

public record BilletResponseDTO(Long id, String code, LocalDateTime dateEmission,
        LocalDateTime dateControle, StatutBillet statut, Long reservationId,
        Long ligneReservationId, Long categorieBilletId, Long evenementId) {}
