package org.example.eventpro.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventpro.dto.request.BilletControleRequest;
import org.example.eventpro.dto.response.BilletResponseDTO;
import org.example.eventpro.service.BilletService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/billets")
@RequiredArgsConstructor
@Tag(name = "Billets", description = "Billets émis après paiement et contrôle à usage unique")
public class BilletController {
    private final BilletService service;

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un billet")
    public BilletResponseDTO findById(@PathVariable Long id) { return service.findById(id); }

    @GetMapping("/code/{code}")
    @Operation(summary = "Consulter un billet par code")
    public BilletResponseDTO findByCode(@PathVariable String code) { return service.findByCode(code); }

    @GetMapping("/reservation/{reservationId}")
    @Operation(summary = "Lister les billets d'une réservation")
    public List<BilletResponseDTO> findByReservation(@PathVariable Long reservationId) {
        return service.findByReservationId(reservationId);
    }

    @PostMapping("/{code}/verifier")
    @Operation(summary = "Vérifier un billet pour un événement sans le consommer")
    public BilletResponseDTO verifier(@PathVariable String code, @Valid @RequestBody BilletControleRequest request) {
        return service.verifier(code, request.evenementId());
    }

    @PostMapping("/{code}/controler")
    @Operation(summary = "Composter le billet une seule fois : VALID vers USED")
    public BilletResponseDTO controler(@PathVariable String code, @Valid @RequestBody BilletControleRequest request) {
        return service.controler(code, request.evenementId());
    }
}
