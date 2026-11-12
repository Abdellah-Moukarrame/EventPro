package org.example.eventpro.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CategorieEvenementUpdateRequest(
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal prixUnitaire,
        @NotNull @PositiveOrZero Integer quantiteTotale,
        @NotNull @Positive Integer quotaMaxParClient) {}
