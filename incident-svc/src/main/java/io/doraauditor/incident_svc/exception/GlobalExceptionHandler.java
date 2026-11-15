package io.doraauditor.incident_svc.exception;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler transforming application domain exceptions and validation errors
 * into standardized RFC 7807 {@link ProblemDetail} responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link IncidentNotFoundException}, returning an HTTP 404 (Not Found) RFC 7807 problem detail.
     *
     * @param ex the exception thrown when an incident UUID cannot be located
     * @return {@link ProblemDetail} with status 404 and error documentation URI
     */
    @ExceptionHandler(IncidentNotFoundException.class)
    public ProblemDetail handleIncidentNotFound(IncidentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Incident Not Found");
        problem.setType(URI.create("https://dora-auditor.io/errors/incident-not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles {@link IdempotencyConflictException}, returning an HTTP 422 (Unprocessable Entity) problem detail.
     * <p>
     * Triggered when an existing {@code Idempotency-Key} is submitted with a modified payload body.
     * </p>
     *
     * @param ex the idempotency conflict exception
     * @return {@link ProblemDetail} with status 422 explaining the hash mismatch
     */
    @ExceptionHandler(IdempotencyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Idempotency Key Conflict");
        problem.setType(URI.create("https://dora-auditor.io/errors/idempotency-conflict"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles {@link IllegalStateTransitionException}, returning an HTTP 409 (Conflict) problem detail.
     *
     * @param ex the illegal state transition exception
     * @return {@link ProblemDetail} with status 409 detailing the prohibited transition
     */
    @ExceptionHandler(IllegalStateTransitionException.class)
    public ProblemDetail handleIllegalTransition(IllegalStateTransitionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Illegal State Transition");
        problem.setType(URI.create("https://dora-auditor.io/errors/illegal-state-transition"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles standard {@link IllegalStateException} from domain entity FSM checks, returning HTTP 409 (Conflict).
     *
     * @param ex the illegal state exception
     * @return {@link ProblemDetail} with status 409
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Illegal State Transition");
        problem.setType(URI.create("https://dora-auditor.io/errors/illegal-state"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles JPA {@link OptimisticLockingFailureException}, returning an HTTP 409 (Conflict) problem detail.
     * <p>
     * Alerts callers that another concurrent process updated the entity version before their request arrived.
     * </p>
     *
     * @param ex the optimistic lock failure exception
     * @return {@link ProblemDetail} with status 409 advising the client to reload and retry
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Concurrent modification detected. The incident has been modified by another process. Please reload and retry."
        );
        problem.setTitle("Optimistic Locking Conflict");
        problem.setType(URI.create("https://dora-auditor.io/errors/optimistic-lock-conflict"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles Jakarta Bean Validation errors from {@code @Valid} annotations, returning HTTP 400 (Bad Request).
     * <p>
     * Includes a breakdown map of field names and their specific validation constraint failure messages.
     * </p>
     *
     * @param ex the method argument validation exception
     * @return {@link ProblemDetail} with status 400 and a {@code fieldErrors} property map
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request payload");
        problem.setTitle("Bad Request");
        problem.setType(URI.create("https://dora-auditor.io/errors/validation-failed"));

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("fieldErrors", errors);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Fallback handler for unhandled generic exceptions, returning an HTTP 500 (Internal Server Error) problem detail.
     *
     * @param ex the unexpected exception
     * @return {@link ProblemDetail} with status 500
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://dora-auditor.io/errors/internal-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
