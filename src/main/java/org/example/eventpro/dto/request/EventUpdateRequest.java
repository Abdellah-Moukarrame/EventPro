package org.example.eventpro.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.example.eventpro.enums.StatutEvenement;

import java.time.Instant;

@Getter
@Setter
public class EventUpdateRequest {

    private StatutEvenement statut;

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
}
