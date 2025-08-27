package org.tsumiyoku.gov.abac;

import lombok.RequiredArgsConstructor;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.stereotype.Service;
import org.tsumiyoku.gov.identity.AssuranceRepo;
import org.tsumiyoku.gov.identity.VCRepo;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessService {
    private final AssuranceRepo assuranceRepo;
    private final VCRepo vcRepo;
    private final Enforcer enforcer;

    public record SubjectCtx(UUID citizenId, int ial, int aal, String citizenship) {
    }

    public void check(UUID citizenId, String obj, String act) {
        var as = assuranceRepo.findById(citizenId)
                .orElseThrow(() -> new IllegalStateException("Assurance missing for " + citizenId));
        boolean hasVC = vcRepo.existsBySubject_IdAndTypeAndStatus(citizenId, "CitizenCredential", "ACTIVE");
        var sub = new SubjectCtx(citizenId, as.getIal(), as.getAal(), hasVC ? "ACTIVE" : "NONE");
        boolean ok = enforcer.enforce(Map.of(
                "citizenId", sub.citizenId(),
                "ial", sub.ial(),
                "aal", sub.aal(),
                "citizenship", sub.citizenship()
        ), obj, act);
        if (!ok) throw new org.springframework.security.access.AccessDeniedException("ABAC deny");
    }
}