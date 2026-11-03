package org.example.eventpro.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LigneReservationRequest(
        @NotNull Long categorieBilletEvenementId,
        @Min(1) int quantite
) {}