// src/main/java/com/state/identity/Assurance.java
package org.tsumiyoku.gov.identity;

import jakarta.persistence.*;
import lombok.*;
import org.tsumiyoku.gov.auth.Citizen;

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
    @Column(name = "citizen_id", columnDefinition = "BINARY(16)")
    private UUID citizenId;     // PK = FK -> citizen.id

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id")
    private Citizen citizen;

    @Column(nullable = false)
    private short ial;          // 1..3

    @Column(nullable = false)
    private short aal;          // 1..3

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void pre() {
        if (updatedAt == null) updatedAt = Instant.now();
        if (ial == 0) ial = 1;
        if (aal == 0) aal = 1;
    }
}