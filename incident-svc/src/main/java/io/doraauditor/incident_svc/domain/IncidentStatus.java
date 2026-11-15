package io.doraauditor.incident_svc.domain;

/**
 * Strict Finite State Machine statuses for an incident.
 * Represents the legal lifecycle from detection to supervisory submission and closure.
 */
public enum IncidentStatus {
    RECEIVED,
    CLASSIFIED,
    DRAFTING,
    HITL_PENDING,
    APPROVED,
    SUBMITTED,
    RESOLVED,
    CLOSED;

    /**
     * Enforces the regulatory transition matrix.
     * Prevents invalid state skips and illegal rollbacks.
     */
    public boolean canTransitionTo(IncidentStatus target) {
        return switch (this) {
            case RECEIVED -> target == CLASSIFIED;
            case CLASSIFIED -> target == DRAFTING || target == CLOSED;
            case DRAFTING -> target == HITL_PENDING;
            case HITL_PENDING -> target == APPROVED || target == DRAFTING;
            case APPROVED -> target == SUBMITTED;
            case SUBMITTED -> target == RESOLVED;
            case RESOLVED -> target == CLOSED;
            case CLOSED -> false;
        };
    }
}
