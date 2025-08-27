package org.tsumiyoku.gov.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.tsumiyoku.gov.auth.CitizenRepo;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SessionAuthFilter extends OncePerRequestFilter {
    private final SessionServerRepo repo;
    private final CitizenRepo citizenRepo;
    @Value("${security.cookie.name:STATESESSID}")
    private String cookieName;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        var cookie = Optional.ofNullable(req.getCookies())
                .flatMap(cs -> Arrays.stream(cs).filter(c -> cookieName.equals(c.getName())).findFirst());
        if (cookie.isPresent()) {
            try {
                byte[] id = Base64.getUrlDecoder().decode(cookie.get().getValue());
                var opt = repo.findActive(id, Instant.now());
                if (opt.isPresent()) {
                    var c = citizenRepo.findById(opt.get().getCitizenId()).orElse(null);
                    if (c != null && "ACTIVE".equals(c.getStatus())) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                new UserPrincipal(c.getId(), c.getEmail()), null, List.of());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        chain.doFilter(req, res);
    }
}