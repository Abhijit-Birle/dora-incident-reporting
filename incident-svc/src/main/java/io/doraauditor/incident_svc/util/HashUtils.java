package io.doraauditor.incident_svc.util;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashUtils {

    public static String computeSha256(String rawJsonPayload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawJsonPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes); // Returns 64-character hex string
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}