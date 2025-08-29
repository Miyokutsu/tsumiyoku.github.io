package org.tsumiyoku.gov.audit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public final class HmacChain {
    private HmacChain() {
    }

    public static byte[] hmac(byte[] key, byte[]... parts) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            for (byte[] p : parts) mac.update(p);
            return mac.doFinal();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] decodeKeyB64(String b64) {
        return Base64.getDecoder().decode(b64);
    }

    public static String b64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }
}