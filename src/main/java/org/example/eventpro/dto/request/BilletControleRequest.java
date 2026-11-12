package org.example.eventpro.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BilletControleRequest(@NotNull @Positive Long evenementId) {}
