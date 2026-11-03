package org.example.eventpro.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReservationCreateRequest(
        @NotNull Long clientId,

        @NotEmpty
        List<@NotNull @Valid LigneReservationRequest> lignes,

        String codePromotion
) {}