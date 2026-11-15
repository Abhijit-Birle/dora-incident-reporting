package io.doraauditor.incident_svc.controller;

import io.doraauditor.incident_svc.domain.IncidentStatus;
import io.doraauditor.incident_svc.dto.IncidentRequest;
import io.doraauditor.incident_svc.dto.IncidentResponse;
import io.doraauditor.incident_svc.dto.TransitionStatusRequest;
import io.doraauditor.incident_svc.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller exposing endpoints for incident ingestion, lifecycle status transitions,
 * regulatory classification inspection, and dual-clock deadline monitoring under EU DORA (Regulation 2022/2554).
 */
@RestController
@RequestMapping("/v1/")
public class IncidentController {

    private final IncidentService incidentService;

    /**
     * Constructs an {@code IncidentController} with the required incident service dependency.
     *
     * @param incidentService the core service handling incident intake, RTS evaluation, and lifecycle operations
     */
    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    /**
     * Ingests a new ICT incident alert with HTTP {@code Idempotency-Key} header support.
     * <p>
     * Evaluates the incident against RTS (EU 2024/1772) major incident criteria, arms the statutory
     * dual-clock reporting deadlines (initial notification due &le; 4h from classification and &le; 24h
     * from awareness), and persists the incident in an initial {@code CLASSIFIED} state.
     * </p>
     * <p>
     * If the caller provides an {@code Idempotency-Key} header, subsequent requests with the identical key
     * and payload return the cached HTTP 201 response with {@code idempotentReplay = true}. If the key is
     * replayed with a differing payload, an {@link io.doraauditor.incident_svc.exception.IdempotencyConflictException}
     * is raised (mapped to HTTP 422 Unprocessable Entity). When the header is omitted, a composite key
     * ({@code source:sourceEventId}) is synthesized as a fallback.
     * </p>
     *
     * @param idempotencyKey optional client-supplied idempotency key (UUID or client transaction identifier)
     * @param request        validated incident intake payload containing telemetry, metadata, and RTS metrics
     * @return {@link ResponseEntity} containing the classified {@link IncidentResponse} with HTTP status 201 (Created)
     */
    @PostMapping("incident")
    public ResponseEntity<IncidentResponse> createIncident(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody IncidentRequest request) {

        // Fallback to domain composite key if caller omitted the header
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = request.source() + ":" + request.sourceEventId();
        }

        IncidentResponse response = incidentService.processIncident(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves an incident by its primary UUID identifier, dynamically recalculating the live statutory countdown.
     * <p>
     * The returned {@link IncidentResponse#deadlineCountdownMinutes()} reflects the remaining minutes until
     * the statutory deadline cutoff relative to the exact moment of request execution.
     * </p>
     *
     * @param incidentId the unique identifier of the incident
     * @return {@link ResponseEntity} containing the {@link IncidentResponse} with HTTP status 200 (OK)
     * @throws io.doraauditor.incident_svc.exception.IncidentNotFoundException if no incident matches the given UUID (mapped to HTTP 404)
     */
    @GetMapping("incident/{incidentId}")
    public ResponseEntity<IncidentResponse> getIncident(@PathVariable UUID incidentId) {
        IncidentResponse response = incidentService.getIncident(incidentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of incidents with optional regulatory filtering by lifecycle status and severity.
     * <p>
     * Supports filtering by {@link IncidentStatus} (e.g., {@code DRAFTING}, {@code HITL_PENDING}) and major
     * incident qualification ({@code isMajor=true|false}). Default pagination sorts descending by creation timestamp.
     * </p>
     *
     * @param status   optional filter restricting results to a specific {@link IncidentStatus}
     * @param isMajor  optional filter restricting results to major ({@code true}) or non-major ({@code false}) incidents
     * @param pageable pagination and sorting parameters (defaults to 20 records per page sorted by {@code createdAt DESC})
     * @return {@link ResponseEntity} containing a paginated {@link Page} of {@link IncidentResponse} records with HTTP status 200 (OK)
     */
    @GetMapping("incidents")
    public ResponseEntity<Page<IncidentResponse>> listIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Boolean isMajor,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<IncidentResponse> response = incidentService.listIncidents(status, isMajor, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Advances an incident through its regulatory lifecycle state machine with optimistic concurrency verification.
     * <p>
     * Requires the caller to supply the current entity {@code version} to prevent lost updates in multi-operator
     * or human-in-the-loop (HITL) compliance workflows. Transitions must conform to the regulatory transition matrix
     * defined in {@link IncidentStatus#canTransitionTo(IncidentStatus)}.
     * </p>
     *
     * @param incidentId the unique identifier of the incident to transition
     * @param request    transition payload containing target status, expected version token, actor ID, and audit rationale
     * @return {@link ResponseEntity} containing the updated {@link IncidentResponse} with HTTP status 200 (OK)
     * @throws io.doraauditor.incident_svc.exception.IncidentNotFoundException        if the incident UUID does not exist (HTTP 404)
     * @throws org.springframework.dao.OptimisticLockingFailureException               if the supplied version does not match the stored version (HTTP 409)
     * @throws io.doraauditor.incident_svc.exception.IllegalStateTransitionException  if the transition violates the lifecycle state machine (HTTP 409)
     */
    @PatchMapping("incident/{incidentId}/status")
    public ResponseEntity<IncidentResponse> transitionStatus(
            @PathVariable UUID incidentId,
            @Valid @RequestBody TransitionStatusRequest request) {

        IncidentResponse response = incidentService.transitionStatus(incidentId, request);
        return ResponseEntity.ok(response);
    }
}
