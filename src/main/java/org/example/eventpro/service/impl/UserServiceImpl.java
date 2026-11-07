
package org.example.eventpro.service.impl;

import lombok.RequiredArgsConstructor;

import org.example.eventpro.dto.request.RegisterRequest;
import org.example.eventpro.dto.request.UtilisateurUpdateRequest;
import org.example.eventpro.dto.response.UtilisateurResponseDTO;

import org.example.eventpro.entity.Client;
import org.example.eventpro.entity.Utilisateur;

import org.example.eventpro.exceptions.BusinessException;
import org.example.eventpro.exceptions.ResourceNotFoundException;

import org.example.eventpro.mapper.UtilisateurMapper;
import org.example.eventpro.repository.UtilisateurRepository;
import org.example.eventpro.service.UtilisateurService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;

    // 1. REGISTER
    @Override
    public UtilisateurResponseDTO register(RegisterRequest request) {

        // Vérifier si l'email est déjà utilisé
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(
                    "Cette adresse email est déjà utilisée"
            );
        }

        // Créer un client
        Client client = new Client();

        client.setNom(request.getNom().trim());
        client.setEmail(email);

        // Ne jamais enregistrer le mot de passe en clair
        client.setMotDePasse(
                passwordEncoder.encode(request.getMotDePasse())
        );

        // Enregistrer dans PostgreSQL
        Utilisateur saved = enregistrer(client);

        // Convertir l'entité en DTO
        return utilisateurMapper.toDTO(saved);
    }

    // 2. FIND BY ID
    @Override
    @Transactional(readOnly = true)
    public UtilisateurResponseDTO findById(Long id) {

        Utilisateur utilisateur = trouverUtilisateur(id);

        return utilisateurMapper.toDTO(utilisateur);
    }

    // 3. FIND ALL
    @Override
    @Transactional(readOnly = true)
    public List<UtilisateurResponseDTO> findAll() {

        return utilisateurRepository.findAll()
                .stream()
                .map(utilisateurMapper::toDTO)
                .toList();
    }

    // 4. UPDATE
    @Override
    public UtilisateurResponseDTO update(
            Long id,
            UtilisateurUpdateRequest request
    ) {

        Utilisateur utilisateur = trouverUtilisateur(id);

        // Vérifier si le nouvel email appartient à un autre utilisateur
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (utilisateurRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {

            throw new BusinessException(
                    "Cette adresse email est déjà utilisée"
            );
        }

        // Modifier uniquement les champs autorisés
        utilisateurMapper.updateEntity(request, utilisateur);
        utilisateur.setNom(request.getNom().trim());
        utilisateur.setEmail(email);

        // Enregistrer les modifications
        Utilisateur saved = enregistrer(utilisateur);

        return utilisateurMapper.toDTO(saved);
    }

    // 5. DELETE
    @Override
    public void delete(Long id) {

        Utilisateur utilisateur = trouverUtilisateur(id);

        try {
            utilisateurRepository.delete(utilisateur);
            utilisateurRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Cet utilisateur est lié à des données existantes et ne peut pas être supprimé");
        }
    }

    private Utilisateur enregistrer(Utilisateur utilisateur) {
        try {
            return utilisateurRepository.saveAndFlush(utilisateur);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Impossible d'enregistrer cet utilisateur : conflit de données (email déjà utilisé)");
        }
    }

    // MÉTHODE PRIVÉE
    private Utilisateur trouverUtilisateur(Long id) {

        return utilisateurRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Utilisateur introuvable : " + id
                        )
                );
    }
}
