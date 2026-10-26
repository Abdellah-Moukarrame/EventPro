package org.example.eventpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.eventpro.dto.request.EventCreateRequest;
import org.example.eventpro.dto.request.EventUpdateRequest;
import org.example.eventpro.dto.response.EventResponseDTO;
import org.example.eventpro.entity.Evenement;
import org.example.eventpro.entity.Organisateur;
import org.example.eventpro.enums.StatutEvenement;
import org.example.eventpro.mapper.EvenementMapper;
import org.example.eventpro.repository.EvenementRepository;
import org.example.eventpro.repository.OrganisateurRepository;
import org.example.eventpro.service.EvenementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EvenementServiceImpl implements EvenementService {

    private final EvenementRepository evenementRepository;
    private final OrganisateurRepository organisateurRepository;
    private final EvenementMapper evenementMapper;

    @Override
    @Transactional
    public EventResponseDTO create(EventCreateRequest request) {

        validateDates(
                request.getDateDebut(),
                request.getDateFin(),
                request.getDateOuvertureVente(),
                request.getDateClotureVente()
        );

        Organisateur organisateur = organisateurRepository
                .findById(request.getOrganisateurId())
                .orElseThrow(() ->
                        new NoSuchElementException("Organisateur introuvable")
                );

        Evenement evenement = evenementMapper.toEntity(request);

        evenement.setOrganisateur(organisateur);
        evenement.setStatut(StatutEvenement.DRAFT);

        return evenementMapper.toDTO(
                evenementRepository.save(evenement)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponseDTO findById(Long id) {

        Evenement evenement = findEntityById(id);

        return evenementMapper.toDTO(evenement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponseDTO> findAll() {

        return evenementRepository.findAll()
                .stream()
                .map(evenementMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public EventResponseDTO update(
            Long id,
            EventUpdateRequest request
    ) {

        Evenement evenement = findEntityById(id);

        validateDates(
                request.getDateDebut(),
                request.getDateFin(),
                request.getDateOuvertureVente(),
                request.getDateClotureVente()
        );

        evenementMapper.updateEntity(request, evenement);

        return evenementMapper.toDTO(
                evenementRepository.save(evenement)
        );
    }

    @Override
    @Transactional
    public void delete(Long id) {

        Evenement evenement = findEntityById(id);

        evenementRepository.delete(evenement);
    }

    private Evenement findEntityById(Long id) {

        return evenementRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Evenement introuvable")
                );
    }

    private void validateDates(
            java.time.Instant debut,
            java.time.Instant fin,
            java.time.Instant ouverture,
            java.time.Instant cloture
    ) {

        if (!fin.isAfter(debut)) {
            throw new IllegalArgumentException(
                    "La date de fin doit être après la date de début"
            );
        }

        if (!cloture.isAfter(ouverture)) {
            throw new IllegalArgumentException(
                    "La clôture des ventes doit être après leur ouverture"
            );
        }

        if (cloture.isAfter(debut)) {
            throw new IllegalArgumentException(
                    "Les ventes doivent se terminer avant le début de l'événement"
            );
        }
    }
}