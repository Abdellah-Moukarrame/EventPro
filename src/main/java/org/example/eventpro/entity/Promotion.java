
package org.example.eventpro.entity;

import org.example.eventpro.enums.TypeReduction;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeReduction typeReduction;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valeurReduction;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Column(nullable = false)
    private int limiteUtilisation;

    @Column(nullable = false)
    private int nombreUtilisations = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id", nullable = false)
    private Evenement evenement;
}
