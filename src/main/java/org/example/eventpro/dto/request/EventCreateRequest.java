package org.example.eventpro.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class EventCreateRequest {

    @NotBlank
    private String titre;

    private String description;

    @NotBlank
    private String lieu;

    @NotNull
    @Positive
    private Integer capacite;

    @NotNull
    private Instant dateDebut;

    @NotNull
    private Instant dateFin;

    @NotNull
    private Instant dateOuvertureVente;

    @NotNull
    private Instant dateClotureVente;

    @NotNull
    private Long organisateurId;
}