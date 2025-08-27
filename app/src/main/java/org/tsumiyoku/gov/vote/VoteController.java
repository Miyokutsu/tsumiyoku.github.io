package org.tsumiyoku.gov.vote;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tsumiyoku.gov.abac.AbacGuard;
import org.tsumiyoku.gov.security.UserPrincipal;
import org.tsumiyoku.gov.vote.dto.VoteRequest;

@RestController
@RequestMapping("/vote")
@RequiredArgsConstructor
public class VoteController {

    private final AbacGuard guard;
    private final VoteService service;

    @PostMapping("/cast")
    public ResponseEntity<Void> cast(@AuthenticationPrincipal UserPrincipal me,
                                     @RequestBody VoteRequest req) {
        // 1) Autorisation par ABAC (IAL>=2, AAL>=2, VC citoyen actif)
        guard.requireCanVote(me.id());
        // 2) Logique métier (unicité, délais...) -> service
        service.cast(me.id(), req.referendumId(), req.choice());
        // 3) 204 No Content
        return ResponseEntity.noContent().build();
    }
}