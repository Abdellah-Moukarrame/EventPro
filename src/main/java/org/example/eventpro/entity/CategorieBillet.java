
package org.example.eventpro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories_billets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategorieBillet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(nullable = false)
    private int quantiteTotale;

    @Column(nullable = false)
    private int quotaMaxParClient;

    // Plusieurs catégories -> un événement
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id", nullable = false)
    private Evenement evenement;

    // Une catégorie -> plusieurs lignes de réservation
    @OneToMany(mappedBy = "categorieBillet")
    private List<LigneReservation> lignesReservation =
            new ArrayList<>();

    // Une catégorie -> plusieurs billets
    @OneToMany(mappedBy = "categorieBillet")
    private List<Billet> billets = new ArrayList<>();
}
