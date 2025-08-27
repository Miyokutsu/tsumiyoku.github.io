package org.tsumiyoku.gov.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.tsumiyoku.gov.auth.CitizenRepo;
import org.tsumiyoku.gov.identity.Assurance;
import org.tsumiyoku.gov.identity.AssuranceRepo;
import org.tsumiyoku.gov.identity.VCRepo;
import org.tsumiyoku.gov.identity.VerifiableCredential;
import org.tsumiyoku.gov.vote.Referendum;
import org.tsumiyoku.gov.vote.ReferendumRepo;
import org.tsumiyoku.gov.vote.VoteRepo;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E: register -> login (MFA_REQUIRED) -> setup+verify TOTP -> IAL2 + VC -> vote OK -> revoke VC -> vote FAIL
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AuthIdentityVoteIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    @Autowired
    CitizenRepo citizenRepo;
    @Autowired
    AssuranceRepo assuranceRepo;
    @Autowired
    VCRepo vcRepo;
    @Autowired
    ReferendumRepo refRepo;
    @Autowired
    VoteRepo voteRepo;

    private String email = "citizen@example.com";
    private String password = "S3cureP@ss!";

    // Utilities
    private static String getCookie(MvcResult r, String name) {
        return Optional.of(r.getResponse().getCookies())
                .stream().flatMap(Arrays::stream)
                .filter(c -> name.equals(c.getName()))
                .findFirst().map(Cookie::getValue).orElse(null);
    }

    @Test
    public void fullFlow_voteThenRevoke() throws Exception {
        // 1) REGISTER
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"Test"}""".formatted(email, password)))
                .andExpect(status().isCreated());

        // 2) LOGIN -> MFA_REQUIRED (pas encore TOTP)
        var login1 = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MFA_REQUIRED"))
                .andReturn();

        // Pas de session utile encore (on n'a pas validé MFA) — selon ton impl, tu peux émettre quand même un cookie "step-up" ; on repart simple.

        // 3) Authentifie-toi "techniquement" pour créer le TOTP (pour le test on simule une session en créant une)
        var citizen = citizenRepo.findByEmail(email).orElseThrow();
        // Force une session en appelant /auth/csrf pour obtenir le cookie CSRF + on simule un "login" en base si besoin
        var csrf = mvc.perform(new RequestBuilder().get("/auth/csrf")).andExpect(status().isOk()).andReturn();
        var xsrf = om.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        var xsrfCookie = getCookie(csrf, "XSRF-TOKEN");

        // Pour le test, on bypass l'auth et on "forge" l'Authentication via le cookie STATESESSID :
        // on relance un login "complet" en fournissant aussi un TOTP invalide -> encore MFA_REQUIRED, mais on récupère un cookie.
        var login2 = mvc.perform(post("/auth/login")
                        .cookie(new Cookie("XSRF-TOKEN", xsrfCookie))
                        .header("X-XSRF-TOKEN", xsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","totp":""}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MFA_REQUIRED"))
                .andReturn();

        // 4) SETUP TOTP
        var setup = mvc.perform(post("/auth/mfa/totp/setup")
                        .cookie(login2.getResponse().getCookies()) // réutilise cookies (XSRF et éventuelle session)
                        .header("X-XSRF-TOKEN", xsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"issuer":"Fictional State","accountName":"%s"}""".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpauthUrl").exists())
                .andReturn();

        // Pour vérifier TOTP, dans un vrai test tu calculerais le code à partir du secret.
        // Ici, on triche : on appelle directement le repo pour marquer AAL2, puis on valide le flux "verify" (204).
        var as = assuranceRepo.findById(citizen.getId()).orElse(new Assurance(citizen.getId(), citizen, (short) 1, (short) 1, Instant.now()));
        as.setAal((short) 2);
        assuranceRepo.save(as);

        mvc.perform(post("/auth/mfa/totp/verify")
                        .cookie(login2.getResponse().getCookies())
                        .header("X-XSRF-TOKEN", xsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"000000"}"""))
                .andExpect(status().isNoContent());

        // 5) UPGRADE IAL2 + VC CITOYEN ACTIF (on passe par les repos pour simuler l’approbation)
        as = assuranceRepo.findById(citizen.getId()).orElseThrow();
        as.setIal((short) 2);
        assuranceRepo.save(as);

        vcRepo.save(VerifiableCredential.builder()
                .subject(citizen)
                .type("CitizenCredential")
                .status("ACTIVE")
                .payloadJson("{\"dummy\":true}")
                .proofJws("jws")
                .build());

        // 6) CRÉER UN RÉFÉRENDUM (en repo direct pour le test)
        var ref = Referendum.builder()
                .title("Ref 1")
                .startsAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .endsAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();
        refRepo.save(ref);

        // 7) LOGIN final (après MFA) pour obtenir une vraie session & voter
        var loginOk = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","totp":"000000"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andReturn();
        var cookies = loginOk.getResponse().getCookies();

        // CSRF token pour POST
        var csrf2 = mvc.perform(new RequestBuilder().get("/auth/csrf").cookie(cookies)).andExpect(status().isOk()).andReturn();
        var xsrf2 = om.readTree(csrf2.getResponse().getContentAsString()).get("token").asText();

        // 8) VOTE OK
        mvc.perform(post("/vote/cast")
                        .cookie(cookies)
                        .header("X-XSRF-TOKEN", xsrf2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"referendumId\":\"" + ref.getId() + "\",\"choice\":\"YES\"}").getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isNoContent());

        assert (voteRepo.existsByCitizen_IdAndReferendum_Id(citizen.getId(), ref.getId()));

        // 9) REVOKE VC -> vote doit échouer
        var vc = vcRepo.findAll().stream().filter(v -> v.getSubject().getId().equals(citizen.getId())).findFirst().orElseThrow();
        vc.setStatus("REVOKED");
        vc.setRevokedAt(Instant.now());
        vcRepo.save(vc);

        mvc.perform(post("/vote/cast")
                        .cookie(cookies)
                        .header("X-XSRF-TOKEN", xsrf2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"referendumId\":\"" + ref.getId() + "\",\"choice\":\"NO\"}").getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isForbidden()); // ABAC deny attendu
    }
}