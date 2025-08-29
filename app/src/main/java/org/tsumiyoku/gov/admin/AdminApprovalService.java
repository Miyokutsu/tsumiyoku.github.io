package org.tsumiyoku.gov.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminApprovalService {
    private final AdminApprovalRepo repo;

    public List<AdminApproval> list(String role) {
        return repo.findActiveByRole(role, Instant.now());
    }

    public AdminApproval get(UUID id) {
        return repo.findById(id).orElseThrow();
    }

    @Transactional
    public AdminApproval upsert(UUID citizenId, UUID approvedBy, List<String> roles, Instant expiresAt) {
        var cur = repo.findRaw(citizenId).orElse(null);
        if (cur == null) {
            cur = AdminApproval.builder().citizenId(citizenId).approvedBy(approvedBy)
                    .roles(roles.toArray(String[]::new)).expiresAt(expiresAt).build();
        } else {
            cur.setApprovedBy(approvedBy);
            cur.setRoles(roles.toArray(String[]::new));
            cur.setExpiresAt(expiresAt);
        }
        return repo.save(cur);
    }

    @Transactional
    public void delete(UUID id) {
        repo.deleteById(id);
    }
}