package io.doraauditor.incident_svc.exception;

import java.util.UUID;

/**
 * Exception thrown when an incident requested by its UUID does not exist.
 * <p>
 * Mapped to HTTP 404 Not Found by {@link GlobalExceptionHandler}.
 * </p>
 */
public class IncidentNotFoundException extends RuntimeException {

    /**
     * Constructs an {@code IncidentNotFoundException} citing the missing incident identifier.
     *
     * @param incidentId the UUID that could not be found
     */
    public IncidentNotFoundException(UUID incidentId) {
        super("Incident with ID '" + incidentId + "' was not found.");
    }
}
