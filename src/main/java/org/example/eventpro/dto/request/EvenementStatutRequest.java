package org.example.eventpro.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.eventpro.enums.StatutEvenement;

public record EvenementStatutRequest(@NotNull StatutEvenement statut) {}
