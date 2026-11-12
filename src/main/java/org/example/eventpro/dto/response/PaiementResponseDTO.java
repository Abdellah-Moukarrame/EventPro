package org.example.eventpro.dto.response;

import org.example.eventpro.enums.StatutPaiement;
import java.math.BigDecimal;
import java.time.Instant;

public record PaiementResponseDTO(Long id, String reference,
        Long reservationId, BigDecimal montant, Instant datePaiement,
        StatutPaiement statut) {}
