package org.tsumiyoku.gov.auth;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InDbCredentialRepo implements CredentialRepository {
    private final WebAuthnCredentialRepo repo; // JPA pour webauthn_credential

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        // username = email ou externalId (à ton choix)
        return repo.findAllByCitizenEmail(username).stream()
                .map(e -> PublicKeyCredentialDescriptor.builder()
                        .id(new ByteArray(e.getCredentialId()))
                        .transports(Optional.ofNullable(e.getTransports()).orElse(new String[0]))
                        .build())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return Optional.empty();
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credId, ByteArray userHandle) {
        return repo.findByCredentialId(credId.getBytes()).map(e ->
                RegisteredCredential.builder()
                        .credentialId(credId)
                        .userHandle(new ByteArray(e.getCitizenId().toString().getBytes(StandardCharsets.UTF_8)))
                        .publicKeyCose(new ByteArray(e.getPublicKeyCose()))
                        .signatureCount(e.getSignCount())
                        .build()
        );
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credId) {
        return Set.of();
    }
}
