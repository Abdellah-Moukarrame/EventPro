package org.example.eventpro.dto.response;

import java.math.BigDecimal;

public record LigneReservationResponseDTO(
        Long id,
        Long categorieBilletEvenementId,
        int quantite,
        BigDecimal prixUnitaire,
        BigDecimal sousTotal
) {}