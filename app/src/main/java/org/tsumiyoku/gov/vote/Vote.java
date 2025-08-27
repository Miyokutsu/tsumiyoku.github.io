package org.tsumiyoku.gov.vote;

import jakarta.persistence.*;
import lombok.*;
import org.tsumiyoku.gov.auth.Citizen;

import java.time.Instant;
import java.util.UUID;

// Vote.java
@Entity
@Table(name = "vote",
        uniqueConstraints = @UniqueConstraint(name = "uk_vote_citizen_ref", columnNames = {"citizen_id", "referendum_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referendum_id", nullable = false)
    private Referendum referendum;
    @Column(nullable = false)
    private String choice;
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void pre() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
