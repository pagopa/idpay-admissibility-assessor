package it.gov.pagopa.common.crypto.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class CryptoUtils {
    private CryptoUtils() {
    }

    public static String sha256Base64(String value) {
        return Base64.getEncoder().encodeToString(sha256(value));
    }

    public static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Something went wrong creating SHA256 digest", e);
        }
    }
}
