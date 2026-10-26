package org.example.eventpro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clients")
@PrimaryKeyJoinColumn(name = "id")
public class Client extends Utilisateur {
    @Column(name ="telephone")
    private String telephone;


}
