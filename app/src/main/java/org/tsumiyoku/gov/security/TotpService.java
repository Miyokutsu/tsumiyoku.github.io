package org.tsumiyoku.gov.security;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@RequiredArgsConstructor
public class TotpService {
    private final MfaTotpRepo repo;
    @Value("${mfa.totp.enc-key}")
    private String encKey; // 32 octets base64 pour AES-GCM

    public record SetupData(String otpauthUrl, String secretBase32) {
    }

    public SetupData beginSetup(UUID citizenId, String issuer, String accountName) {
        byte[] secret = new byte[20];
        new SecureRandom().nextBytes(secret);
        String base32 = com.eatthepath.otp.TimeBasedOneTimePasswordGenerator KeyGenerators.generateBase32Key(20); // ou util perso
        // Remplace par base32 ci-dessus si tu préfères
        base32 = Base32String.encode(secret); // si tu as un util Base32 (Google)
        String otpauth = "otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=6&period=30"
                .formatted(URLEncoder.encode(issuer, UTF_8), URLEncoder.encode(accountName, UTF_8),
                        base32, URLEncoder.encode(issuer, UTF_8));
        var enc = aesGcmEncrypt(secret, Base64.getDecoder().decode(encKey));
        repo.save(new MfaTotp(citizenId, enc, null, null));
        return new SetupData(otpauth, base32);
    }

    public boolean verify(UUID citizenId, String code) {
        var m = repo.findById(citizenId).orElseThrow();
        byte[] secret = aesGcmDecrypt(m.getSecretEnc(), Base64.getDecoder().decode(encKey));
        var totp = new TimeBasedOneTimePasswordGenerator(); // default 30s/6digits
        var now = Instant.now();
        // fenêtre ±1 pas
        return Stream.of(-1, 0, 1).anyMatch(i -> {
            Instant t = now.plusSeconds(30L * i);
            int expected;
            try {
                expected = totp.generateOneTimePassword(secret, Date.from(t));
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
            return String.format("%06d", expected).equals(code);
        });
    }

    // AES-GCM util — à implémenter (nonce 12o + tag 16o)
    private static byte[] aesGcmEncrypt(byte[] plaintext, byte[] key) { /* ... */
        return new byte[0];
    }

    private static byte[] aesGcmDecrypt(byte[] ciphertext, byte[] key) { /* ... */
        return new byte[0];
    }
}