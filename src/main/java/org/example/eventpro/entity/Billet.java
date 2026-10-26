
package org.example.eventpro.entity;

import org.example.eventpro.enums.StatutBillet;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "billets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Billet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private LocalDateTime dateEmission;

    private LocalDateTime dateControle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutBillet statut = StatutBillet.VALID;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_reservation_id", nullable = false)
    private LigneReservation ligneReservation;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_billet_id", nullable = false)
    private CategorieBillet categorieBillet;
}
