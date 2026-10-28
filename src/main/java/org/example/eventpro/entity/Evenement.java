
package org.example.eventpro.entity;

import org.example.eventpro.enums.StatutEvenement;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evenements")
@Getter
@Setter
@NoArgsConstructor
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String lieu;

    @Column(nullable = false)
    private Instant dateDebut;

    @Column(nullable = false)
    private Instant dateFin;

    @Column(nullable = false)
    private Instant dateOuvertureVente;

    @Column(nullable = false)
    private Instant dateClotureVente;

    @Column(nullable = false)
    private int capacite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEvenement statut = StatutEvenement.DRAFT;

    // Plusieurs événements peuvent appartenir à un organisateur
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Organisateur organisateur;

    // Un événement possède plusieurs catégories de billets
    // avec leurs propres prix, stocks et quotas
    @OneToMany(
            mappedBy = "evenement",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CategorieBilletEvenement> categories = new ArrayList<>();

    // Maintenir la cohérence de la relation bidirectionnelle
    public void ajouterCategorie(CategorieBilletEvenement categorie) {
        categories.add(categorie);
        categorie.setEvenement(this);
    }

    public void supprimerCategorie(CategorieBilletEvenement categorie) {
        categories.remove(categorie);
        categorie.setEvenement(null);
    }
}
