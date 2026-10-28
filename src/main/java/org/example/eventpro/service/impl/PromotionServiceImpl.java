
package org.example.eventpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.eventpro.dto.request.PromotionUpdateRequest;
import org.example.eventpro.dto.request.PromotionCreateRequest;
import org.example.eventpro.dto.response.PromotionResponseDTO;
import org.example.eventpro.entity.Evenement;
import org.example.eventpro.entity.Promotion;
import org.example.eventpro.enums.TypeReduction;
import org.example.eventpro.exceptions.BusinessException;
import org.example.eventpro.exceptions.ResourceNotFoundException;
import org.example.eventpro.mapper.PromotionMapper;
import org.example.eventpro.repository.EvenementRepository;
import org.example.eventpro.repository.PromotionRepository;
import org.example.eventpro.service.PromotionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final EvenementRepository evenementRepository;
    private final PromotionMapper promotionMapper;

    @Override
    public PromotionResponseDTO create(PromotionCreateRequest request) {

        // 1. Vérifier que l'événement existe
        Evenement evenement = evenementRepository
                .findById(request.evenementId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Événement introuvable : " + request.evenementId()
                ));

        // 2. Vérifier l'unicité du code pour cet événement
        if (promotionRepository.existsByEvenementIdAndCode(
                evenement.getId(), request.code())) {
            throw new BusinessException(
                    "Ce code promotionnel existe déjà pour cet événement"
            );
        }

        // 3. Valider les règles métier
        validerPromotion(
                request.typeReduction(),
                request.valeurReduction(),
                request.dateDebut(),
                request.dateFin(),
                request.limiteUtilisation()
        );

        // 4. Convertir le DTO en entité
        Promotion promotion = promotionMapper.toEntity(request);

        // 5. Associer l'événement
        promotion.setEvenement(evenement);
        promotion.setNombreUtilisations(0);

        // 6. Enregistrer la promotion
        Promotion saved = promotionRepository.save(promotion);

        return promotionMapper.toDTO(saved);
    }

    @Override
    public PromotionResponseDTO update(
            Long id,
            PromotionUpdateRequest request
    ) {
        Promotion promotion = trouverPromotion(id);

        validerPromotion(
                request.typeReduction(),
                request.valeurReduction(),
                request.dateDebut(),
                request.dateFin(),
                request.limiteUtilisation()
        );

        // Ne pas réduire la limite sous les utilisations existantes
        if (request.limiteUtilisation()
                < promotion.getNombreUtilisations()) {
            throw new BusinessException(
                    "La limite ne peut pas être inférieure au nombre d'utilisations"
            );
        }

        // Vérifier si le nouveau code est déjà utilisé
        if (!promotion.getCode().equals(request.code())
                && promotionRepository.existsByEvenementIdAndCode(
                        promotion.getEvenement().getId(),
                        request.code())) {
            throw new BusinessException(
                    "Ce code existe déjà pour cet événement"
            );
        }

        promotionMapper.updateEntity(request, promotion);

        return promotionMapper.toDTO(promotionRepository.save(promotion));
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponseDTO findById(Long id) {
        return promotionMapper.toDTO(trouverPromotion(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> findByEvenementId(Long evenementId) {
        return promotionRepository.findByEvenementId(evenementId)
                .stream()
                .map(promotionMapper::toDTO)
                .toList();
    }

    @Override
    public void delete(Long id) {
        Promotion promotion = trouverPromotion(id);
        promotionRepository.delete(promotion);
    }

    private Promotion trouverPromotion(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Promotion introuvable : " + id
                ));
    }

    private void validerPromotion(
            TypeReduction typeReduction,
            BigDecimal valeurReduction,
            java.time.LocalDate dateDebut,
            java.time.LocalDate dateFin,
            int limiteUtilisation
    ) {
        if (typeReduction == null || valeurReduction == null
                || dateDebut == null || dateFin == null) {
            throw new BusinessException(
                    "Les informations de la promotion sont incomplètes"
            );
        }

        if (dateFin.isBefore(dateDebut)) {
            throw new BusinessException(
                    "La date de fin doit être postérieure ou égale à la date de début"
            );
        }

        if (valeurReduction.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "La réduction doit être supérieure à zéro"
            );
        }

        if (typeReduction == TypeReduction.PERCENTAGE
                && valeurReduction.compareTo(
                        BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException(
                    "Une réduction en pourcentage ne peut pas dépasser 100 %"
            );
        }

        if (limiteUtilisation < 1) {
            throw new BusinessException(
                    "La limite d'utilisation doit être supérieure à zéro"
            );
        }
    }
}
