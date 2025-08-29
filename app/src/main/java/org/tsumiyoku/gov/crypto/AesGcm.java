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
    private static final int NONCE_LEN = 12;
    private static final int TAG_BITS = 128;

    private AesGcm() {
    }

    public static String generateKeyBase64() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(ALG);
            kg.init(256);
            SecretKey k = kg.generateKey();
            return Base64.getEncoder().encodeToString(k.getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] encrypt(byte[] plaintext, byte[] key) {
        try {
            byte[] nonce = new byte[NONCE_LEN];
            new SecureRandom().nextBytes(nonce);
            Cipher c = Cipher.getInstance(TRANSFORM);
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALG), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ct = c.doFinal(plaintext);
            byte[] out = new byte[NONCE_LEN + ct.length];
            System.arraycopy(nonce, 0, out, 0, NONCE_LEN);
            System.arraycopy(ct, 0, out, NONCE_LEN, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] decrypt(byte[] in, byte[] key) {
        try {
            if (in.length < NONCE_LEN + 16) throw new IllegalArgumentException("input too short");
            byte[] nonce = java.util.Arrays.copyOfRange(in, 0, NONCE_LEN);
            byte[] ct = java.util.Arrays.copyOfRange(in, NONCE_LEN, in.length);
            Cipher c = Cipher.getInstance(TRANSFORM);
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ALG), new GCMParameterSpec(TAG_BITS, nonce));
            return c.doFinal(ct);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}