package org.tsumiyoku.gov.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_server")
@Getter
@Setter
public class SessionServer {
    @Id
    @Column(columnDefinition = "BYTEA")
    private byte[] id;
    @Column(name = "citizen_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID citizenId;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant lastSeenAt;
    @Column(nullable = false)
    private Instant expiresAt;
    private byte[] ipHash;
    private byte[] uaHash;
    @Column(nullable = false)
    private byte[] csrfSecret;
    @Column(nullable = false)
    private boolean revoked = false;
}