package io.doraauditor.incident_svc.dto;

import io.doraauditor.incident_svc.domain.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for transitioning an incident through its regulatory lifecycle.
 * Enforces JPA optimistic concurrency control via the version field.
 */
public record TransitionStatusRequest(
        @NotNull(message = "Target status is required")
        IncidentStatus targetStatus,

        @NotNull(message = "Version is required for optimistic concurrency control")
        Long version,

        @Size(max = 512, message = "Reason must not exceed 512 characters")
        String reason,

        @Size(max = 128, message = "Actor ID must not exceed 128 characters")
        String actorId
) {}
