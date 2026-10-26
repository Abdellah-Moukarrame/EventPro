package org.example.eventpro.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.example.eventpro.enums.StatutEvenement;

import java.time.Instant;

@Getter
@Setter
public class EventResponseDTO {

    private Long id;
    private String titre;
    private String description;
    private String lieu;
    private Integer capacite;

    private Instant dateDebut;
    private Instant dateFin;
    private Instant dateOuvertureVente;
    private Instant dateClotureVente;

    private StatutEvenement statut;
    private Long organisateurId;
}