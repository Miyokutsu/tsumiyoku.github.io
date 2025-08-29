package org.tsumiyoku.gov.identity;

import jakarta.persistence.*;
import lombok.*;
import org.tsumiyoku.gov.user.Citizen;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assurance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assurance {
    @Id
    @Column(name = "citizen_id", columnDefinition = "uuid")
    private UUID id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "citizen_id")
    private Citizen citizen;

    private short ial;
    private short aal;
    private Instant updatedAt;
}