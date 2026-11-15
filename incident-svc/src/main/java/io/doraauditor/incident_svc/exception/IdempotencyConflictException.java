package io.doraauditor.incident_svc.exception;

/**
 * Exception thrown when an incoming request reuses an existing idempotency key with a differing payload fingerprint.
 * <p>
 * Mapped to HTTP 422 Unprocessable Entity by {@link GlobalExceptionHandler}.
 * </p>
 */
public class IdempotencyConflictException extends RuntimeException {

    /**
     * Constructs an {@code IdempotencyConflictException} with the specified detail message.
     *
     * @param message description of the key and payload mismatch
     */
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
