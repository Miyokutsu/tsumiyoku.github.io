package org.tsumiyoku.gov.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class AesGcm {
    private static final String ALG = "AES";
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int NONCE_LEN = 12;      // 96-bit
    private static final int TAG_BITS = 128;     // 16 bytes tag

    private AesGcm() {
    }

    /**
     * Génère une clé AES-256 aléatoire (base64).
     */
    public static String generateKeyBase64() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(ALG);
            kg.init(256);
            SecretKey key = kg.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Chiffre (nonce||cipher) où cipher contient le tag GCM à la fin.
     */
    public static byte[] encrypt(byte[] plaintext, byte[] key) {
        try {
            byte[] nonce = new byte[NONCE_LEN];
            new SecureRandom().nextBytes(nonce);
            Cipher c = Cipher.getInstance(TRANSFORM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BITS, nonce);
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALG), spec);
            byte[] ct = c.doFinal(plaintext);
            byte[] out = new byte[NONCE_LEN + ct.length];
            System.arraycopy(nonce, 0, out, 0, NONCE_LEN);
            System.arraycopy(ct, 0, out, NONCE_LEN, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Attend le format nonce||cipher (cipher=texte+tag).
     */
    public static byte[] decrypt(byte[] nonceAndCipher, byte[] key) {
        try {
            if (nonceAndCipher.length < NONCE_LEN + 16) throw new IllegalArgumentException("input too short");
            byte[] nonce = new byte[NONCE_LEN];
            byte[] ct = new byte[nonceAndCipher.length - NONCE_LEN];
            System.arraycopy(nonceAndCipher, 0, nonce, 0, NONCE_LEN);
            System.arraycopy(nonceAndCipher, NONCE_LEN, ct, 0, ct.length);
            Cipher c = Cipher.getInstance(TRANSFORM);
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ALG), new GCMParameterSpec(TAG_BITS, nonce));
            return c.doFinal(ct);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}