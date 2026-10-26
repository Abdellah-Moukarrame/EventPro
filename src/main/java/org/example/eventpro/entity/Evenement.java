
package org.example.eventpro.entity;

import org.example.eventpro.enums.StatutEvenement;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evenements")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private LocalDateTime dateDebut;

    @Column(nullable = false)
    private LocalDateTime dateFin;

    @Column(nullable = false)
    private LocalDateTime dateOuvertureVente;

    @Column(nullable = false)
    private LocalDateTime dateClotureVente;

    @Column(nullable = false)
    private int capacite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEvenement statut = StatutEvenement.DRAFT;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Organisateur organisateur;


    @OneToMany(
            mappedBy = "evenement",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CategorieBillet> categories =
            new ArrayList<>();
}
