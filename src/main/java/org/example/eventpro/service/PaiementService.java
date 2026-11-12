package org.example.eventpro.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.example.eventpro.dto.request.PaymentRequest;
import org.example.eventpro.dto.response.PaiementResponseDTO;

public interface PaiementService {
    PaymentResult pay(@NotNull @Valid PaymentRequest request);
    PaiementResponseDTO findById(Long id);
    PaiementResponseDTO findByReservationId(Long reservationId);
    record PaymentResult(PaiementResponseDTO paiement, boolean created) {}
}
