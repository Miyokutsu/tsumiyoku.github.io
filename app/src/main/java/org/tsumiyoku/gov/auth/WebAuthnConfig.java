package org.tsumiyoku.gov.auth;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class WebAuthnConfig {
    @Bean
    public RelyingParty rp() {
        var identity = RelyingPartyIdentity.builder()
                .id("gov.example.com")      // rpId = domaine
                .name("Fictional State")
                .build();
        return RelyingParty.builder()
                .identity(identity)
                .credentialRepository(new InDbCredentialRepo()) // à écrire (lit webauthn_credential)
                .origins(Set.of("https://gov.example.com"))
                .build();
    }
}
