
package org.example.eventpro.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @OneToMany(mappedBy = "categorieBillet")
    private List<CategorieBilletEvenement> categoriesEvenements =
            new ArrayList<>();
}
