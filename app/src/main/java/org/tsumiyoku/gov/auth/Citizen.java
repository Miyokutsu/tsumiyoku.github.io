// src/main/java/com/state/auth/Citizen.java
package org.tsumiyoku.gov.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "citizen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Citizen {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(unique = true)
    private String externalId;

    @Column(unique = true, nullable = false)
    private String email;

    private Instant emailVerifiedAt;
    private String displayName;

    @Column(nullable = false)
    private String status; // ACTIVE | SUSPENDED | DELETED

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void pre() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "ACTIVE";
    }
}