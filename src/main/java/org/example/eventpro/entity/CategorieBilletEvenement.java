
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id", nullable = false)
    private Evenement evenement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_billet_id", nullable = false)
    private CategorieBillet categorieBillet;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(nullable = false)
    private int quantiteTotale;

    @Column(nullable = false)
    private int quotaMaxParClient;

    @OneToMany(mappedBy = "categorieBilletEvenement")
    private List<LigneReservation> lignesReservation =
            new ArrayList<>();
}
