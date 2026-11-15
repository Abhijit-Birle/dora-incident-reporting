package io.doraauditor.incident_svc.exception;

/**
 * Exception thrown when an attempted incident lifecycle transition violates the state machine rules.
 * <p>
 * Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 * </p>
 */
public class IllegalStateTransitionException extends RuntimeException {

    /**
     * Constructs an {@code IllegalStateTransitionException} with the specified detail message.
     *
     * @param message description of the invalid source and target state transition
     */
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
