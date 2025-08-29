package org.tsumiyoku.gov.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AuditVerifyController {
    private final AuditVerifyService verifier;

    @GetMapping("/verify")
    @PreAuthorize("hasRole('OWNER')")
    public AuditVerifyService.Report verify(@RequestParam(defaultValue = "50") int maxFindings) {
        return verifier.verifyAll(Math.max(1, Math.min(maxFindings, 500)));
    }
}