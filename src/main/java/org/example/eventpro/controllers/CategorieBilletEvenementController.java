package org.example.eventpro.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventpro.dto.request.CategorieEvenementCreateRequest;
import org.example.eventpro.dto.request.CategorieEvenementUpdateRequest;
import org.example.eventpro.dto.response.CategorieEvenementResponseDTO;
import org.example.eventpro.service.CategorieBilletEvenementService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories-billets-evenements")
@RequiredArgsConstructor
@Tag(name = "Catégories de billets par événement")
public class CategorieBilletEvenementController {
    private final CategorieBilletEvenementService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategorieEvenementResponseDTO create(@Valid @RequestBody CategorieEvenementCreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<CategorieEvenementResponseDTO> findAll(@RequestParam(required = false) Long evenementId) {
        return service.findAll(evenementId);
    }

    @GetMapping("/{id}")
    public CategorieEvenementResponseDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public CategorieEvenementResponseDTO update(@PathVariable Long id, @Valid @RequestBody CategorieEvenementUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
