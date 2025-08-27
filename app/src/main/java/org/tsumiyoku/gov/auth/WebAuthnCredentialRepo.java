package org.tsumiyoku.gov.auth;

import java.util.Optional;

public class WebAuthnCredentialRepo {
    public Optional<WebauthnCredential> findAllByCitizenEmail(String username) {
        return Optional.empty();
    }

    public Optional<WebauthnCredential> findByCredentialId(byte[] bytes) {
        return Optional.empty();
    }

    public void save(WebauthnCredential cred) {
    }
}
