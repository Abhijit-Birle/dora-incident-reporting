package io.doraauditor.incident_svc.service;

import io.doraauditor.incident_svc.domain.IncidentStatus;
import io.doraauditor.incident_svc.dto.IncidentRequest;
import io.doraauditor.incident_svc.dto.IncidentResponse;
import io.doraauditor.incident_svc.dto.TransitionStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IncidentService {

    /**
     * Ingests, idempotently deduplicates, classifies, and tracks deadlines for an incoming incident.
     */
    IncidentResponse processIncident(String idempotencyKey, IncidentRequest request);

    /**
     * Retrieves an incident by its UUID, recalculating live countdown slack time.
     */
    IncidentResponse getIncident(UUID incidentId);

    /**
     * Lists incidents with optional status/severity filters and pagination.
     */
    Page<IncidentResponse> listIncidents(IncidentStatus status, Boolean isMajor, Pageable pageable);

    /**
     * Transitions an incident to the next lifecycle state, enforcing FSM and optimistic locking.
     */
    IncidentResponse transitionStatus(UUID incidentId, TransitionStatusRequest request);
}
