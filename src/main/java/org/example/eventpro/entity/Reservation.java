
package org.example.eventpro.entity;

import org.example.eventpro.enums.StatutReservation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private Instant dateCreation;

    @Column(nullable = false)
    private Instant dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutReservation statut =
            StatutReservation.PENDING_PAYMENT;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montantTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @OneToMany(
            mappedBy = "reservation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<LigneReservation> lignesReservation =
            new ArrayList<>();

    @OneToOne(mappedBy = "reservation", fetch = FetchType.LAZY)
    private Paiement paiement;
}
