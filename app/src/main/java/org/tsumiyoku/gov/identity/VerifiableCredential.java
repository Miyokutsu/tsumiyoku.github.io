// src/main/java/com/state/identity/VerifiableCredential.java
package org.tsumiyoku.gov.identity;

import jakarta.persistence.*;
import lombok.*;
import org.tsumiyoku.gov.auth.Citizen;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verifiable_credential")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifiableCredential {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Citizen subject;

    @Column(nullable = false)
    private String type;   // "CitizenCredential" | "ResidentVerified" | ...

    @Column(nullable = false)
    private String status; // ACTIVE | REVOKED

    @Column(nullable = false)
    private Instant issuedAt;

    private Instant revokedAt;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @Lob
    @Column(nullable = false)
    private String proofJws;

    @PrePersist
    void pre() {
        if (id == null) id = UUID.randomUUID();
        if (issuedAt == null) issuedAt = Instant.now();
        if (status == null) status = "ACTIVE";
    }
}