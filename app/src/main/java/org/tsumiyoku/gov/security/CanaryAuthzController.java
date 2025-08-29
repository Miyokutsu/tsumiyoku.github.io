package org.tsumiyoku.gov.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tsumiyoku.gov.admin.AdminApprovalRepo;
import org.tsumiyoku.gov.identity.AssuranceRepo;
import org.tsumiyoku.gov.identity.VCRepo;

import java.time.Instant;

@RestController
@RequestMapping("/authz")
@RequiredArgsConstructor
public class CanaryAuthzController {

    private final AssuranceRepo assuranceRepo;
    private final VCRepo vcRepo;
    private final AdminApprovalRepo approvalRepo;
    private final AuditService audit;

    @GetMapping("/canary")
    public ResponseEntity<Void> canary(@AuthenticationPrincipal OAuth2User me) {
        if (me == null) return ResponseEntity.status(401).build();
        var citizenId = java.util.UUID.fromString((String) me.getAttributes().get("citizenId"));

        var as = assuranceRepo.findById(citizenId).orElse(null);
        boolean ial2 = as != null && as.getIal() >= 2;
        boolean aal2 = as != null && as.getAal() >= 2;
        boolean hasVC = vcRepo.existsBySubject_IdAndTypeAndStatus(citizenId, "CitizenCredential", "ACTIVE");
        var appr = approvalRepo.findById(citizenId).orElse(null);
        boolean adminOk = appr != null &&
                (appr.getExpiresAt() == null || appr.getExpiresAt().isAfter(Instant.now())) &&
                java.util.Arrays.asList(appr.getRoles()).contains("ADMIN");

        if (ial2 && aal2 && hasVC && adminOk) {
            audit.append(citizenId, "CANARY_AUTHZ_ALLOW", true, java.util.Map.of());
            return ResponseEntity.noContent().build();
        }
        audit.append(citizenId, "CANARY_AUTHZ_DENY", false, java.util.Map.of("ial2", ial2, "aal2", aal2, "vc", hasVC, "adminOk", adminOk));
        return ResponseEntity.status(403).build();
    }
}