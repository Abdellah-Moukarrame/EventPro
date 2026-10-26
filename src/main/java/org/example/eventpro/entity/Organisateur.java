package org.example.eventpro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organisateurs")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
public class Organisateur extends Utilisateur {

    @OneToMany(mappedBy = "organisateur")
    private List<Evenement> evenements = new ArrayList<>();
}