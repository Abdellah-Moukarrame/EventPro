package org.example.eventpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.eventpro.dto.request.PaymentRequest;
import org.example.eventpro.dto.response.PaiementResponseDTO;
import org.example.eventpro.exceptions.ReservationExpireeException;
import org.example.eventpro.service.PaiementService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PaiementServiceImpl implements PaiementService {
    private final PaiementTransaction transaction;
    private final ReservationLifecycle lifecycle;

    @Override
    public PaymentResult pay(PaymentRequest request) {
        lifecycle.expire(request.reservationId());
        try {
            return transaction.pay(request);
        } catch (ReservationExpireeException ex) {
            lifecycle.expire(request.reservationId());
            throw ex;
        }
    }

    @Override
    public PaiementResponseDTO findById(Long id) { return transaction.findById(id); }

    @Override
    public PaiementResponseDTO findByReservationId(Long id) { return transaction.findByReservationId(id); }
}
