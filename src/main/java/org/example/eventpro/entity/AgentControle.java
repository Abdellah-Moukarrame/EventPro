
package org.example.eventpro.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agents_controle")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentControle extends Utilisateur {

}
