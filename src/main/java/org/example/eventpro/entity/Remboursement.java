
package org.example.eventpro.entity;

import org.example.eventpro.enums.StatutRemboursement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "remboursements")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Remboursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false)
    private String motif;

    @Column(nullable = false)
    private Instant dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutRemboursement statut =
            StatutRemboursement.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paiement_id", nullable = false)
    private Paiement paiement;
}
