package io.doraauditor.incident_svc.util;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Cryptographic utility providing deterministic SHA-256 fingerprinting for idempotency verification.
 */
public class HashUtils {

    private HashUtils() {
        // Utility class
    }

    /**
     * Computes the SHA-256 digest of the provided string and returns it as a 64-character lowercase hex string.
     * <p>
     * Used for HTTP request body fingerprinting to detect payload tampering or conflicting reuses
     * of the same {@code Idempotency-Key}.
     * </p>
     *
     * @param rawJsonPayload the raw or serialized JSON string to digest
     * @return 64-character lowercase hexadecimal hash representation
     * @throws RuntimeException if SHA-256 message digest algorithm is unavailable in the JVM runtime
     */
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