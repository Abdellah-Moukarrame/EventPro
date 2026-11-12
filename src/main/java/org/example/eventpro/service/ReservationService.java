package org.example.eventpro.service;

import org.example.eventpro.dto.request.ReservationCreateRequest;
import org.example.eventpro.dto.response.ReservationResponseDTO;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface ReservationService {
    ReservationResponseDTO create(@NotNull @Valid ReservationCreateRequest request);
    ReservationResponseDTO findById(Long id);
    ReservationResponseDTO findByReference(String reference);
    List<ReservationResponseDTO> findByClientId(Long clientId);
    void cancel(Long id);
}
