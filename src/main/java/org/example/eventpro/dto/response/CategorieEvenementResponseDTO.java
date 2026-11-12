package org.example.eventpro.dto.response;

import java.math.BigDecimal;

public record CategorieEvenementResponseDTO(Long id, Long evenementId, Long categorieBilletId,
        String nom, BigDecimal prixUnitaire, int quantiteTotale, int quotaMaxParClient) {}
