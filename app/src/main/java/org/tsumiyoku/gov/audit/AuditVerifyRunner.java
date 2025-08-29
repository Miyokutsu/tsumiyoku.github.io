package org.tsumiyoku.gov.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditVerifyRunner implements CommandLineRunner {
    private final AuditVerifyService verifier;

    @Override
    public void run(String... args) {
        if (!"true".equalsIgnoreCase(System.getenv().getOrDefault("AUDIT_VERIFY_ON_BOOT", "false"))) return;
        var rep = verifier.verifyAll(20);
        System.out.println("[AuditVerify] ok=" + rep.isOk() + " total=" + rep.getTotalRows() + " issues=" + rep.getIssues());
        if (!rep.isOk())
            rep.getFindings().forEach(f -> System.out.println(" - id=" + f.getId() + " reason=" + f.getReason() + " details=" + f.getDetails()));
    }
}