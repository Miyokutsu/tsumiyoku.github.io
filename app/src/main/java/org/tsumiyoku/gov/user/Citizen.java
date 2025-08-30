package org.tsumiyoku.gov.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @GeneratedValue
    private UUID id;

    @JdbcTypeCode(SqlTypes.OTHER)
    @Column(name = "email", columnDefinition = "citext", nullable = false, unique = true)
    private String email;

    @Column(unique = true, nullable = false)
    private String externalId;
    private String displayName;
    private String status;
    private Instant createdAt;
}