package org.tsumiyoku.gov.admin;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_approval")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminApproval {
    @Id
    @Column(name = "citizen_id", columnDefinition = "uuid")
    private UUID citizenId;

    @Column(name = "approved_by", columnDefinition = "uuid", nullable = false)
    private UUID approvedBy;

    @Column(name = "roles", nullable = false)
    private String[] roles;

    private Instant expiresAt;
    private Instant createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = Instant.now();
        if (roles == null) roles = new String[]{"ADMIN"};
    }
}