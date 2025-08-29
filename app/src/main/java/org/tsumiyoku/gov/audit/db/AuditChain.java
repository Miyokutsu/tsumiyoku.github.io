package org.tsumiyoku.gov.audit.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_chain")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditChain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "uuid")
    private UUID citizenId;
    private String event;
    private boolean success;
    @Column(columnDefinition = "text")
    private String meta; // store JSON as text
    private Instant createdAt;
    @Column(columnDefinition = "bytea")
    private byte[] prevHmac;
    @Column(columnDefinition = "bytea")
    private byte[] hmac;
}