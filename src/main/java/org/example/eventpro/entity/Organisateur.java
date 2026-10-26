package org.example.eventpro.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
@Entity
@Table(name = "organisateurs")
public class Organisateur extends Utilisateur {
    @OneToMany(mappedBy = "organisateur")
    private List<Evenement> evenements = new ArrayList<>();

    @OneToMany(mappedBy = "organisateur")
    private List<Promotion> promotions = new ArrayList<>();
}
