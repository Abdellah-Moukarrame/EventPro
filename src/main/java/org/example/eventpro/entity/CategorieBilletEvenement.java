
package org.example.eventpro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "categories_billets_evenements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_evenement_categorie",
                columnNames = {"evenement_id", "categorie_billet_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategorieBilletEvenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plusieurs catégories configurées appartiennent à un événement
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evenement_id", nullable = false)
    private Evenement evenement;

    // Une catégorie générale peut être utilisée dans plusieurs événements
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_billet_id", nullable = false)
    private CategorieBillet categorieBillet;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(nullable = false)
    private int quantiteTotale;

    @Column(nullable = false)
    private int quotaMaxParClient;

    // Une catégorie configurée peut apparaître
    // dans plusieurs lignes de réservation
    @OneToMany(mappedBy = "categorieBilletEvenement")
    private List<LigneReservation> lignesReservation = new ArrayList<>();

    public CategorieBilletEvenement(
            CategorieBillet categorieBillet,
            BigDecimal prixUnitaire,
            int quantiteTotale,
            int quotaMaxParClient
    ) {
        this.categorieBillet = categorieBillet;
        this.prixUnitaire = prixUnitaire;
        this.quantiteTotale = quantiteTotale;
        this.quotaMaxParClient = quotaMaxParClient;
    }
}
