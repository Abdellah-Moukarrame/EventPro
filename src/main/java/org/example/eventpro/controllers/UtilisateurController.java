package org.example.eventpro.controllers;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.example.eventpro.dto.request.RegisterRequest;
import org.example.eventpro.service.UtilisateurService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public class UtilisateurController {
    private final UtilisateurService utilisateurService;
    @PostMapping
    @Operation(summary = "Creer un utilisateur (register)")
    public ResponseEntity<RegisterRequest>create(@Valid @RequestBody RegisterRequest request){

    }
}
