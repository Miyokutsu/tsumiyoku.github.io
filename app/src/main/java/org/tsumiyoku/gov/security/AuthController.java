package org.tsumiyoku.gov.security;

import de.mkammerer.argon2.Argon2;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.WebUtils;
import org.tsumiyoku.gov.auth.Citizen;
import org.tsumiyoku.gov.auth.CitizenRepo;

import java.time.Instant;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final CitizenRepo citizenRepo;
    private final CredentialPasswordRepo credRepo;
    private final SessionService sessions;
    private final Argon2 argon2;

    record RegisterReq(String email, String password, String displayName) {
    }

    record LoginReq(String email, String password, String totp) {
    }

    record LoginResp(String status) {
    } // OK | MFA_REQUIRED

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterReq req) {
        var c = Citizen.builder().email(req.email()).displayName(req.displayName()).status("ACTIVE").createdAt(Instant.now()).build();
        citizenRepo.save(c);
        var hash = argon2.hash(3, 1 << 16, 1, req.password()); // à calibrer prod (~100-250ms)
        credRepo.save(new CredentialPassword(c.getId(), hash, "argon2id", Instant.now(), 0, null));
        // TODO: email_token VERIFY_EMAIL
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(HttpServletRequest request, HttpServletResponse response,
                                   @RequestBody LoginReq req) {
        var c = citizenRepo.findByEmail(req.email()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        var cred = credRepo.findById(c.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!argon2.verify(cred.getPasswordHash(), req.password())) {
            // TODO: failed_count/lockout
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        // MFA check (TOTP d’abord, WebAuthn sinon via endpoints dédiés)
        boolean hasTotp = /* lookup mfa_totp exists & verified */ false;
        if (hasTotp) {
            if (req.totp() == null || !totpService.verify(c.getId(), req.totp())) {
                return ResponseEntity.ok(new LoginResp("MFA_REQUIRED"));
            }
        }
        // ROTATION: révoquer ancienne session si présente (optionnel)
        var oldCookie = WebUtils.getCookie(request, "STATESESSID");
        if (oldCookie != null) sessions.revoke(oldCookie.getValue());

        // Issue nouvelle session & set cookie
        String sid = sessions.issueSession(c.getId(), request.getRemoteAddr(), request.getHeader("User-Agent"));
        var cookie = sessions.buildCookie(sid, request.isSecure());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new LoginResp("OK"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        var cookie = WebUtils.getCookie(req, "STATESESSID");
        if (cookie != null) sessions.revoke(cookie.getValue());
        var expired = ResponseCookie.from("STATESESSID", "").path("/").httpOnly(true).maxAge(0).build();
        res.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}