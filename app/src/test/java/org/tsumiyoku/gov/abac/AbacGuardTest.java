// src/test/java/com/state/abac/AbacGuardTest.java
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AbacGuardTest {

    @Autowired
    AbacGuard guard;
    @Autowired
    CitizenRepo citizenRepo;
    @Autowired
    AssuranceRepo assuranceRepo;
    @Autowired
    VCRepo vcRepo;

    @Test
    void guardBlocksWhenVCRevoked() {
        var c = Citizen.builder().email("c@example.com").status("ACTIVE").createdAt(Instant.now()).build();
        citizenRepo.save(c);

        assuranceRepo.save(Assurance.builder()
                .citizenId(c.getId()).citizen(c)
                .ial((short) 2).aal((short) 2).updatedAt(Instant.now()).build());

        var vc = VerifiableCredential.builder()
                .subject(c).type("CitizenCredential").status("REVOKED")
                .payloadJson("{}").proofJws("jws").build();
        vcRepo.save(vc);

        var ex = assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> guard.requireCanVote(c.getId()));
        assertTrue(ex.getMessage().contains("ABAC deny"));
    }
}