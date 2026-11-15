package io.doraauditor.incident_svc.service;

import io.doraauditor.incident_svc.domain.IncidentStatus;
import io.doraauditor.incident_svc.dto.IncidentRequest;
import io.doraauditor.incident_svc.dto.IncidentResponse;
import io.doraauditor.incident_svc.dto.TransitionStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service contract governing ICT incident ingestion, deterministic RTS classification,
 * dual-clock statutory deadline monitoring, and regulatory lifecycle state transitions.
 */
public interface IncidentService {

    /**
     * Ingests, deduplicates, classifies, and initializes tracking for an incoming ICT incident.
     * <p>
     * Follows a 6-step transactional pipeline:
     * <ol>
     *   <li><b>Idempotency Check:</b> Verifies the provided idempotency key against the cache. Replays
     *       return cached responses. Mismatched payload hashes raise an {@link io.doraauditor.incident_svc.exception.IdempotencyConflictException}.</li>
     *   <li><b>Source Event Deduplication:</b> Secondary deduplication on {@code (source, sourceEventId)} prevents duplicate entity creation.</li>
     *   <li><b>RTS Classification:</b> Evaluates the incident against RTS (EU 2024/1772) Article 9 specification rules.</li>
     *   <li><b>Dual-Clock Deadlines:</b> Calculates statutory cutoff {@code min(classifiedAt + 4h, awarenessAt + 24h)} for major incidents.</li>
     *   <li><b>Entity Persistence:</b> Saves the incident entity in {@link IncidentStatus#CLASSIFIED} state with version 0.</li>
     *   <li><b>Response Caching:</b> Stores the classified response in the idempotency table with a 24-hour TTL.</li>
     * </ol>
     * </p>
     *
     * @param idempotencyKey unique idempotency token (from HTTP header or synthesized fallback)
     * @param request        incoming incident attributes, telemetry, and regulatory metrics
     * @return {@link IncidentResponse} containing classification outcome, criteria met, and deadline countdown
     * @throws io.doraauditor.incident_svc.exception.IdempotencyConflictException if the idempotency key was previously submitted with a different payload
     */
    IncidentResponse processIncident(String idempotencyKey, IncidentRequest request);

    /**
     * Retrieves an incident by its unique UUID, dynamically computing live statutory countdown slack time.
     *
     * @param incidentId the unique identifier of the incident to find
     * @return {@link IncidentResponse} representing the incident with refreshed countdown minutes
     * @throws io.doraauditor.incident_svc.exception.IncidentNotFoundException if no incident matches the given identifier
     */
    IncidentResponse getIncident(UUID incidentId);

    /**
     * Lists incidents matching optional regulatory status and major incident severity filters with pagination.
     *
     * @param status   optional filter restricting results to an {@link IncidentStatus}
     * @param isMajor  optional filter restricting results to major ({@code true}) or non-major ({@code false}) incidents
     * @param pageable pagination parameters including page index, size, and sort direction
     * @return a {@link Page} of {@link IncidentResponse} objects matching the criteria
     */
    Page<IncidentResponse> listIncidents(IncidentStatus status, Boolean isMajor, Pageable pageable);

    /**
     * Transitions an incident through its regulatory lifecycle state machine, enforcing optimistic concurrency.
     * <p>
     * Compares the client-provided version token with the stored entity version to prevent concurrent modification
     * anomalies. Validates the transition against {@link IncidentStatus#canTransitionTo(IncidentStatus)}.
     * </p>
     *
     * @param incidentId the unique identifier of the incident to transition
     * @param request    transition request specifying the target status, expected entity version, actor, and rationale
     * @return {@link IncidentResponse} reflecting the updated entity state and incremented version
     * @throws io.doraauditor.incident_svc.exception.IncidentNotFoundException        if the incident is not found
     * @throws org.springframework.dao.OptimisticLockingFailureException               if the version token does not match the database version
     * @throws IllegalStateException                                                   if the requested state transition is not permitted by the state machine
     */
    IncidentResponse transitionStatus(UUID incidentId, TransitionStatusRequest request);
}
