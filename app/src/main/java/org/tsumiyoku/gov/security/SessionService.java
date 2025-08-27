package org.tsumiyoku.gov.security;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionServerRepo repo; // JPA pour session_server
    @Value("${security.session.idle-seconds:1800}")
    private long idleSeconds;
    @Value("${security.session.absolute-seconds:86400}")
    private long absoluteSeconds;
    @Value("${security.cookie.name:STATESESSID}")
    private String cookieName;

    @Transactional
    public String issueSession(UUID citizenId, String ip, String ua) {
        var idBytes = new byte[32];
        new SecureRandom().nextBytes(idBytes);
        var csrf = new byte[32];
        new SecureRandom().nextBytes(csrf);
        var now = Instant.now();
        var sess = new SessionServer();
        sess.setId(idBytes);
        sess.setCitizenId(citizenId);
        sess.setCreatedAt(now);
        sess.setLastSeenAt(now);
        sess.setExpiresAt(now.plusSeconds(Math.min(idleSeconds, absoluteSeconds))); // idle init
        sess.setCsrfSecret(csrf);
        // ip_hash / ua_hash = HMAC stocké si tu veux
        repo.save(sess);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(idBytes);
    }

    @Transactional
    public void revoke(String sessionIdB64) {
        repo.findById(Base64.getUrlDecoder().decode(sessionIdB64))
                .ifPresent(s -> {
                    s.setRevoked(true);
                });
    }

    public ResponseCookie buildCookie(String sessionId, boolean secure) {
        return ResponseCookie.from(cookieName, sessionId)
                .httpOnly(true).secure(secure).sameSite("Strict")
                .path("/").maxAge(Duration.ofSeconds(absoluteSeconds)).build();
    }
}