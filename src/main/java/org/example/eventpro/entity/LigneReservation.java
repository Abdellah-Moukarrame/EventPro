
package org.example.eventpro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lignes_reservation")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LigneReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int quantite;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sousTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "categorie_billet_evenement_id",
            nullable = false
    )
    private CategorieBilletEvenement categorieBilletEvenement;

    @OneToMany(mappedBy = "ligneReservation")
    private List<Billet> billets = new ArrayList<>();

    public BigDecimal calculerSousTotal() {
        return prixUnitaire.multiply(
                BigDecimal.valueOf(quantite)
        );
    }
}
