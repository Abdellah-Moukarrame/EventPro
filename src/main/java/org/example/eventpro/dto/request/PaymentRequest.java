package org.example.eventpro.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull @Positive Long reservationId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal montant
) {}
