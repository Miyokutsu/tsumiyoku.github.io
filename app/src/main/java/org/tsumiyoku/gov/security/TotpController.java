package org.tsumiyoku.gov.security;

import lombok.RequiredArgsConstructor;
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

import java.time.Instant;

@RestController
@RequestMapping("/auth/mfa/totp")
@RequiredArgsConstructor
public class TotpController {
    private final TotpService totp;
    private final AssuranceRepo assuranceRepo;

    record SetupReq(String issuer, String accountName) {
    }

    record VerifyReq(String code) {
    }

    record SetupResp(String otpauthUrl) {
    }

    @PostMapping("/setup")
    public SetupResp setup(@AuthenticationPrincipal UserPrincipal me, @RequestBody SetupReq req) {
        var data = totp.beginSetup(me.id(), req.issuer(), req.accountName());
        return new SetupResp(data.otpauthUrl());
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verify(@AuthenticationPrincipal UserPrincipal me, @RequestBody VerifyReq req) {
        if (!totp.verify(me.id(), req.code())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var as = assuranceRepo.findById(me.id()).orElse(new Assurance(me.id(), null, (short) 1, (short) 1, Instant.now()));
        if (as.getAal() < 2) {
            as.setAal((short) 2);
            as.setUpdatedAt(Instant.now());
        }
        assuranceRepo.save(as);
        return ResponseEntity.noContent().build();
    }
}
