package org.tsumiyoku.gov.security;

import org.springframework.security.core.AuthenticatedPrincipal;

import java.util.UUID;

public record UserPrincipal(UUID id, String email) implements AuthenticatedPrincipal {
    @Override
    public String getName() {
        return email;
    }
}