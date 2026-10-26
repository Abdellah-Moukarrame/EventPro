package org.example.eventpro.entity;

import jakarta.persistence.*;
import lombok.*;
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

@Entity
@Table(name="utilisateurs")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom ;

    @Column(nullable = false , unique = true)
    private String email ;

}
