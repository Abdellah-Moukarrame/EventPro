package org.example.eventpro.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.example.eventpro.dto.request.PaymentRequest;
import org.example.eventpro.dto.response.PaiementResponseDTO;
import org.example.eventpro.service.PaiementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
@Tag(name = "Paiements", description = "Paiement du montant d’une réservation")
public class PaymentController {
    private final PaiementService service;

    @PostMapping
    @Operation(summary = "Traiter un paiement (201 nouveau résultat, 200 répétition identique)")
    public ResponseEntity<PaiementResponseDTO> pay(@Valid @RequestBody PaymentRequest request) {
        return response(service.pay(request));
    }

    public record MontantRequest(@NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal montant) {}

    @PostMapping("/reservation/{reservationId}")
    @Operation(summary = "Payer la réservation avec son montant")
    public ResponseEntity<PaiementResponseDTO> payReservation(@PathVariable Long reservationId,
            @Valid @RequestBody MontantRequest request) {
        return response(service.pay(new PaymentRequest(reservationId, request.montant())));
    }

    private ResponseEntity<PaiementResponseDTO> response(PaiementService.PaymentResult result) {
        return result.created()
                ? ResponseEntity.created(URI.create("/api/paiements/" + result.paiement().id())).body(result.paiement())
                : ResponseEntity.ok(result.paiement());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un paiement")
    public PaiementResponseDTO findById(@PathVariable Long id) { return service.findById(id); }

    @GetMapping("/reservation/{reservationId}")
    @Operation(summary = "Consulter le paiement d'une réservation")
    public PaiementResponseDTO findByReservationId(@PathVariable Long reservationId) {
        return service.findByReservationId(reservationId);
    }
}
