
package org.example.eventpro.dto.request;

import jakarta.validation.constraints.*;
import org.example.eventpro.enums.TypeReduction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromotionUpdateRequest(

        @NotBlank(message = "Le code est obligatoire")
        String code,

        @NotNull(message = "Le type de réduction est obligatoire")
        TypeReduction typeReduction,

        @NotNull(message = "La valeur de réduction est obligatoire")
        @DecimalMin(value = "0.01")
        BigDecimal valeurReduction,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin,

        @Min(value = 1)
        int limiteUtilisation

) {}
