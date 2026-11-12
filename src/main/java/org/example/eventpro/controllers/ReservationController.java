package org.example.eventpro.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventpro.dto.request.ReservationCreateRequest;
import org.example.eventpro.dto.response.ReservationResponseDTO;
import org.example.eventpro.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Réservations", description = "Réservation temporaire de billets et annulation")
public class ReservationController {
    private final ReservationService service;

    @PostMapping
    @Operation(summary = "Réserver des billets pour un événement, avec promotion optionnelle")
    public ResponseEntity<ReservationResponseDTO> create(@Valid @RequestBody ReservationCreateRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/reservations/" + result.id())).body(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une réservation")
    public ReservationResponseDTO findById(@PathVariable Long id) { return service.findById(id); }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Consulter une réservation par référence")
    public ReservationResponseDTO findByReference(@PathVariable String reference) {
        return service.findByReference(reference);
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Lister les réservations d'un client")
    public List<ReservationResponseDTO> findByClient(@PathVariable Long clientId) {
        return service.findByClientId(clientId);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Annuler une réservation non payée, sans double libération")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }


}
