package io.doraauditor.incident_svc.domain;

/**
 * Strict Finite State Machine (FSM) statuses governing the regulatory lifecycle of an ICT incident.
 * <p>
 * Represents the sequence from initial ingestion through deterministic classification, automated report drafting,
 * Human-in-the-Loop (HITL) compliance sign-off, competent authority submission, and ultimate incident closure.
 * </p>
 */
public enum IncidentStatus {

    /** Incident telemetry ingested from upstream observability (Datadog, PagerDuty, Prometheus). */
    RECEIVED,

    /** Incident evaluated against RTS (EU 2024/1772) specification criteria; deadlines armed. */
    CLASSIFIED,

    /** AI report agent drafting initial/intermediate regulatory notification under RTS template. */
    DRAFTING,

    /** Draft notification queued for Human-in-the-Loop compliance officer review and signature. */
    HITL_PENDING,

    /** Compliance officer signed off on notification content; ready for supervisory dispatch. */
    APPROVED,

    /** Notification successfully dispatched to the National Competent Authority (NCA / ECB / EBA). */
    SUBMITTED,

    /** Underlying technical root cause mitigated; service operations fully restored. */
    RESOLVED,

    /** Final regulatory report submitted; audit trail sealed. Terminal state. */
    CLOSED;

    /**
     * Enforces the regulatory state transition matrix for incident lifecycle management.
     * <p>
     * Prohibits invalid state skips (e.g. {@code DRAFTING} &rarr; {@code SUBMITTED}) and illegal rollbacks,
     * ensuring compliance auditability. A return to {@code DRAFTING} is permitted from {@code HITL_PENDING}
     * when a reviewer requests revisions.
     * </p>
     *
     * @param target the proposed target {@link IncidentStatus}
     * @return {@code true} if the transition is legally valid according to the matrix, {@code false} otherwise
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
