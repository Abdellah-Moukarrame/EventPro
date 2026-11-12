package org.example.eventpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.eventpro.dto.request.CategorieEvenementCreateRequest;
import org.example.eventpro.dto.request.CategorieEvenementUpdateRequest;
import org.example.eventpro.dto.response.CategorieEvenementResponseDTO;
import org.example.eventpro.entity.*;
import org.example.eventpro.exceptions.BusinessException;
import org.example.eventpro.exceptions.ResourceNotFoundException;
import org.example.eventpro.mapper.CategorieEvenementMapper;
import org.example.eventpro.repository.*;
import org.example.eventpro.service.CategorieBilletEvenementService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;
import static org.example.eventpro.enums.StatutReservation.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CategorieBilletEvenementServiceImpl implements CategorieBilletEvenementService {
    private final EvenementRepository evenements;
    private final TicketCategoryRepository catalogue;
    private final CategorieBilletEvenementRepository categories;
    private final LigneReservationRepository lignes;
    private final CategorieEvenementMapper mapper;
    private final Clock clock;

    public CategorieEvenementResponseDTO create(CategorieEvenementCreateRequest request) {
        var evenement = verrouillerEvenement(request.evenementId());
        var nom = request.nom().strip();
        if (categories.existsByEvenementIdAndCategorieBilletNomIgnoreCase(evenement.getId(), nom)) {
            throw new BusinessException("Cette catégorie est déjà configurée pour cet événement");
        }
        verifierCapacite(evenement, 0, request.quantiteTotale());
        var generale = catalogue.findFirstByNomIgnoreCaseOrderByIdAsc(nom).orElseGet(() -> {
            var nouvelle = BeanUtils.instantiateClass(CategorieBillet.class);
            nouvelle.setNom(nom);
            return catalogue.save(nouvelle);
        });
        var categorie = new CategorieBilletEvenement(generale, request.prixUnitaire(),
                request.quantiteTotale(), request.quotaMaxParClient());
        categorie.setEvenement(evenement);
        return mapper.toDTO(categories.save(categorie));
    }

    @Transactional(readOnly = true)
    public List<CategorieEvenementResponseDTO> findAll(Long evenementId) {
        if (evenementId != null && !evenements.existsById(evenementId)) {
            throw new ResourceNotFoundException("Événement introuvable : " + evenementId);
        }
        var resultat = evenementId == null ? categories.findAll() : categories.findByEvenementIdOrderByIdAsc(evenementId);
        return resultat.stream().map(mapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public CategorieEvenementResponseDTO findById(Long id) {
        return mapper.toDTO(categories.findById(id).orElseThrow(() -> introuvable(id)));
    }

    public CategorieEvenementResponseDTO update(Long id, CategorieEvenementUpdateRequest request) {
        Long evenementId = categories.findEvenementId(id).orElseThrow(() -> introuvable(id));
        var evenement = verrouillerEvenement(evenementId);
        var categorie = categories.findWithLockById(id).orElseThrow(() -> introuvable(id));
        verifierCapacite(evenement, categorie.getQuantiteTotale(), request.quantiteTotale());
        long occupee = lignes.quantiteOccupee(id, null, clock.instant(), CONFIRMED, PENDING_PAYMENT);
        if (request.quantiteTotale() < occupee) {
            throw new BusinessException("Le stock ne peut pas être inférieur aux places déjà réservées ou vendues");
        }
        categorie.setPrixUnitaire(request.prixUnitaire());
        categorie.setQuantiteTotale(request.quantiteTotale());
        categorie.setQuotaMaxParClient(request.quotaMaxParClient());
        return mapper.toDTO(categorie);
    }

    public void delete(Long id) {
        var categorie = categories.findWithLockById(id).orElseThrow(() -> introuvable(id));
        if (lignes.existsByCategorieBilletEvenementId(id)) {
            throw new BusinessException("Cette catégorie est liée à une réservation");
        }
        categories.delete(categorie);
    }

    private Evenement verrouillerEvenement(Long id) {
        return evenements.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Événement introuvable : " + id));
    }

    private void verifierCapacite(Evenement evenement, int ancienneQuantite, int nouvelleQuantite) {
        if (categories.quantiteConfiguree(evenement.getId()) - ancienneQuantite + nouvelleQuantite > evenement.getCapacite()) {
            throw new BusinessException("Le stock des catégories dépasse la capacité de l'événement");
        }
    }

    private ResourceNotFoundException introuvable(Long id) {
        return new ResourceNotFoundException("Catégorie d'événement introuvable : " + id);
    }
}
