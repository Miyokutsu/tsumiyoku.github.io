// src/test/java/com/state/abac/AccessServiceTest.java
package org.tsumiyoku.gov.abac;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.tsumiyoku.gov.auth.Citizen;
import org.tsumiyoku.gov.auth.CitizenRepo;
import org.tsumiyoku.gov.identity.Assurance;
import org.tsumiyoku.gov.identity.AssuranceRepo;
import org.tsumiyoku.gov.identity.VCRepo;
import org.tsumiyoku.gov.identity.VerifiableCredential;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AccessServiceTest {

    @Autowired
    CitizenRepo citizenRepo;
    @Autowired
    AssuranceRepo assuranceRepo;
    @Autowired
    VCRepo vcRepo;
    @Autowired
    AccessService access;

    private UUID makeCitizen(String email) {
        var c = Citizen.builder()
                .email(email)
                .displayName("User")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();
        citizenRepo.save(c);
        // default assurance (ial=1, aal=1)
        var as = Assurance.builder()
                .citizenId(c.getId())
                .citizen(c)
                .ial((short) 1).aal((short) 1)
                .updatedAt(Instant.now())
                .build();
        assuranceRepo.save(as);
        return c.getId();
    }

    @Test
    void voteDenied_whenNotCitizenOrAAL2() {
        var uid = makeCitizen("a@example.com");
        // nothing else (IAL1/AAL1, no VC)
        var ex = assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> access.check(uid, "vote", "cast"));
        assertTrue(ex.getMessage().contains("ABAC deny"));
    }

    @Test
    void voteAllowed_whenIAL2_AAL2_andCitizenVC() {
        var uid = makeCitizen("b@example.com");

        // Upgrade assurance to IAL2/AAL2
        var as = assuranceRepo.findById(uid).orElseThrow();
        as.setIal((short) 2);
        as.setAal((short) 2);
        assuranceRepo.save(as);

        // Issue an ACTIVE CitizenCredential
        var vc = VerifiableCredential.builder()
                .subject(citizenRepo.findById(uid).orElseThrow())
                .type("CitizenCredential")
                .status("ACTIVE")
                .payloadJson("{\"dummy\":true}")
                .proofJws("jws")
                .build();
        vcRepo.save(vc);

        // Should pass
        assertDoesNotThrow(() -> access.check(uid, "vote", "cast"));
    }
}