
package org.example.eventpro.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.example.eventpro.dto.request.RegisterRequest;
import org.example.eventpro.entity.Client;
import org.example.eventpro.entity.Administrateur;
import org.example.eventpro.entity.AgentControle;
import org.example.eventpro.entity.Organisateur;
import org.example.eventpro.dto.request.UtilisateurUpdateRequest;
import java.util.List;
import org.example.eventpro.dto.response.UtilisateurResponseDTO;
import org.example.eventpro.service.UtilisateurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponseDTO createAdmin(@Valid @RequestBody RegisterRequest request) {
        return utilisateurService.createAdministrateur(request);
    }

    @PostMapping("/agent")
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponseDTO createAgent(@Valid @RequestBody RegisterRequest request) {
        return utilisateurService.createAgentControle(request);
    }

    @PostMapping("/organisateur")
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponseDTO createOrganisateur(@Valid @RequestBody RegisterRequest request) {
        return utilisateurService.createOrganisateur(request);
    }

    @GetMapping
    @Operation(summary = "Lister les utilisateurs")
    public List<UtilisateurResponseDTO> findAll() {
        return utilisateurService.findAll();
    }

    @GetMapping("/clients")
    @Operation(summary = "Lister les clients")
    public List<UtilisateurResponseDTO> findClients() {
        return utilisateurService.findByType(Client.class);
    }

    @GetMapping("/admins")
    @Operation(summary = "Lister les admins")
    public List<UtilisateurResponseDTO> findAdmins() {
        return utilisateurService.findByType(Administrateur.class);
    }

    @GetMapping("/agents")
    @Operation(summary = "Lister les agents")
    public List<UtilisateurResponseDTO> findAgents() {
        return utilisateurService.findByType(AgentControle.class);
    }

    @GetMapping("/organisateurs")
    @Operation(summary = "Lister les organisateurs")
    public List<UtilisateurResponseDTO> findOrganisateurs() {
        return utilisateurService.findByType(Organisateur.class);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un utilisateur")
    public UtilisateurResponseDTO findById(@PathVariable Long id) {
        return utilisateurService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier le nom et l'email d'un utilisateur")
    public UtilisateurResponseDTO update(@PathVariable Long id,
            @Valid @RequestBody UtilisateurUpdateRequest request) {
        return utilisateurService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        utilisateurService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    @Operation(summary = "Créer un utilisateur")
    public ResponseEntity<UtilisateurResponseDTO> create(
            @Valid @RequestBody RegisterRequest request
    ) {
        UtilisateurResponseDTO response =
                utilisateurService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
