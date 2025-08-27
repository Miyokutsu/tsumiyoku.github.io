package org.tsumiyoku.gov.auth;

import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.tsumiyoku.gov.identity.Assurance;
import org.tsumiyoku.gov.identity.AssuranceRepo;
import org.tsumiyoku.gov.security.SessionService;
import org.tsumiyoku.gov.security.UserPrincipal;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth/webauthn")
@RequiredArgsConstructor
public class WebAuthnController {
    private final RelyingParty rp;
    private final WebAuthnCredentialRepo credRepo;
    private final CitizenRepo citizenRepo;
    private final AssuranceRepo assuranceRepo;
    private final Map<UUID, PublicKeyCredentialCreationOptions> creationCache = new ConcurrentHashMap<>();
    private final Map<UUID, AssertionRequest> assertionCache = new ConcurrentHashMap<>();

    @PostMapping("/register/challenge")
    public PublicKeyCredentialCreationOptions registerChallenge(@AuthenticationPrincipal UserPrincipal me) {
        var userIdentity = UserIdentity.builder()
                .name(me.email()) // username
                .displayName(me.email())
                .id(new ByteArray(me.id().toString().getBytes(StandardCharsets.UTF_8)))
                .build();
        var options = rp.startRegistration(StartRegistrationOptions.builder()
                .user(userIdentity)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .userVerification(AuthenticatorSelectionCriteria.UserVerificationRequirement.PREFERRED).build())
                .build());
        creationCache.put(me.id(), options);
        return options;
    }

    @PostMapping("/register/finish")
    public ResponseEntity<Void> registerFinish(@AuthenticationPrincipal UserPrincipal me,
                                               @RequestBody PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc) throws RegistrationFailedException {
        var options = creationCache.remove(me.id());
        var result = rp.finishRegistration(FinishRegistrationOptions.builder()
                .request(options).response(pkc).build());
        // Persister la clé
        var cred = new WebauthnCredential();
        cred.setCitizenId(me.id());
        cred.setCredentialId(result.getKeyId().getId().getBytes());
        cred.setPublicKeyCose(result.getAttestation().getAuthenticatorData().getAttestedCredentialData().get().getCredentialPublicKey().getBytes());
        cred.setSignCount(result.getAttestation().getAuthenticatorData().getSignatureCounter());
        cred.setCreatedAt(Instant.now());
        credRepo.save(cred);

        // Upgrade AAL
        var as = assuranceRepo.findById(me.id()).orElse(new Assurance(me.id(), null, (short) 1, (short) 1, Instant.now()));
        if (as.getAal() < 2) {
            as.setAal((short) 2);
            as.setUpdatedAt(Instant.now());
        }
        assuranceRepo.save(as);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assert/challenge")
    public AssertionRequest assertChallenge(@RequestBody Map<String, String> body) {
        var username = body.get("email");
        var req = rp.startAssertion(StartAssertionOptions.builder().username(username).build());
        // Associer au compte ciblé si tu forces username = email existant
        var citizen = citizenRepo.findByEmail(username).orElseThrow();
        assertionCache.put(citizen.getId(), req);
        return req;
    }

    @PostMapping("/assert/finish")
    public ResponseEntity<?> assertFinish(HttpServletRequest request, HttpServletResponse response,
                                          @RequestBody PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc) throws AssertionFailedException {
        var reqObj = assertionCache.values().stream().findFirst().orElseThrow(); // pour un PoC; sinon mappe par user
        var result = rp.finishAssertion(FinishAssertionOptions.builder().request(reqObj).response(pkc).build());
        if (!result.isSuccess()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        // Le user est: result.getUsername() -> retrouve citizenId, émettre session
        var citizen = citizenRepo.findByEmail(result.getUsername()).orElseThrow();
        String sid = sessions.issueSession(citizen.getId(), request.getRemoteAddr(), request.getHeader("User-Agent"));
        response.addHeader(HttpHeaders.SET_COOKIE, sessions.buildCookie(sid, request.isSecure()).toString());
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}