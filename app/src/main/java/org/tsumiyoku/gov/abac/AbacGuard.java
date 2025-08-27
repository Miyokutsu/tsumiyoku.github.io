package org.tsumiyoku.gov.abac;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AbacGuard {
    private final AccessService access;

    public void requireCanVote(UUID citizenId) {
        access.check(citizenId, "vote", "cast");
    }
}