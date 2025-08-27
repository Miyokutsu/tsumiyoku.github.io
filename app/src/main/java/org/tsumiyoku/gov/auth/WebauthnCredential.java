package org.tsumiyoku.gov.auth;

import com.yubico.webauthn.data.AuthenticatorTransport;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class WebauthnCredential {
    UUID citizenId;
    byte[] credentialId;
    Instant createdAt;
    byte[] publicKeyCose;
    long signCount;
    Set<AuthenticatorTransport> transports;
}
