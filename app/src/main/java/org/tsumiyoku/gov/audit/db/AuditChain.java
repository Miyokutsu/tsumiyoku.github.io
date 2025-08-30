package org.tsumiyoku.gov.audit.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @Column(name = "meta", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String meta;
    @Column(columnDefinition = "text")
    private Instant createdAt;
    @Column(columnDefinition = "bytea")
    private byte[] prevHmac;
    @Column(columnDefinition = "bytea")
    private byte[] hmac;
}