package org.example.eventpro.service;

import org.example.eventpro.dto.response.BilletResponseDTO;
import java.util.List;

public interface BilletService {
    // Internal operation: no public endpoint can mint tickets.
    List<BilletResponseDTO> generateForReservation(Long reservationId);
    BilletResponseDTO findById(Long id);
    BilletResponseDTO findByCode(String code);
    List<BilletResponseDTO> findByReservationId(Long reservationId);
    BilletResponseDTO verifier(String code, Long evenementId);
    BilletResponseDTO controler(String code, Long evenementId);
}
