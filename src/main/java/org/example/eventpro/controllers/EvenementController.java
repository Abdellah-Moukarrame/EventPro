package org.example.eventpro.controllers;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.example.eventpro.dto.request.EventCreateRequest;
import org.example.eventpro.dto.request.EventUpdateRequest;
import org.example.eventpro.dto.request.EvenementStatutRequest;
import org.example.eventpro.dto.response.EventResponseDTO;
import org.example.eventpro.service.EvenementService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evenements")
@Tag(name = "Événements")
@RequiredArgsConstructor
public class EvenementController {

    private final EvenementService evenementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDTO create(
            @Valid @RequestBody EventCreateRequest request
    ) {
        return evenementService.create(request);
    }

    @GetMapping
    public List<EventResponseDTO> findAll() {
        return evenementService.findAll();
    }

    @GetMapping("/{id}")
    public EventResponseDTO findById(@PathVariable Long id) {
        return evenementService.findById(id);
    }

    @PutMapping("/{id}")
    public EventResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody EventUpdateRequest request
    ) {
        return evenementService.update(id, request);
    }

    @PatchMapping("/{id}/statut")
    public EventResponseDTO updateStatut(@PathVariable Long id, @Valid @RequestBody EvenementStatutRequest request) {
        return evenementService.updateStatut(id, request.statut());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        evenementService.delete(id);
    }
}
