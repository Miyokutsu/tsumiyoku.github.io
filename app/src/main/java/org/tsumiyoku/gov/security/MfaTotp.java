package org.tsumiyoku.gov.security;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mfa_totp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MfaTotp {
    @Id
    @Column(name = "citizen_id", columnDefinition = "BINARY(16)")
    private UUID citizenId;
    @Lob
    @Column(name = "secret_enc", nullable = false)
    private byte[] secretEnc;
    private Instant verifiedAt;
    private Instant lastUsedAt;
}