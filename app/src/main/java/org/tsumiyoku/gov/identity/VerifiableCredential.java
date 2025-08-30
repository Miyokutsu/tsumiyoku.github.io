package org.tsumiyoku.gov.identity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.tsumiyoku.gov.user.Citizen;

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
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Citizen subject;

    private String type;
    private String status;
    private Instant issuedAt;
    private Instant revokedAt;

    @Column(name = "payload_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payloadJson;

    private String proofJws;
}