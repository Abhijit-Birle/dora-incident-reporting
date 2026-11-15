package io.doraauditor.incident_svc.exception;

import java.util.UUID;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(UUID incidentId) {
        super("Incident with ID '" + incidentId + "' was not found.");
    }
}
