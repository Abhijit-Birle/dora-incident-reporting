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

@RestController
@RequestMapping("/v1/")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    /**
     * Ingests a new incident alert with HTTP Idempotency-Key support.
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
     * Retrieves an incident by UUID with live deadline countdown recalculation.
     */
    @GetMapping("incident/{incidentId}")
    public ResponseEntity<IncidentResponse> getIncident(@PathVariable UUID incidentId) {
        IncidentResponse response = incidentService.getIncident(incidentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists incidents with optional regulatory filters (status, isMajor) and pagination.
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
     * Transitions an incident through its FSM lifecycle stages with optimistic locking verification.
     */
    @PatchMapping("incident/{incidentId}/status")
    public ResponseEntity<IncidentResponse> transitionStatus(
            @PathVariable UUID incidentId,
            @Valid @RequestBody TransitionStatusRequest request) {

        IncidentResponse response = incidentService.transitionStatus(incidentId, request);
        return ResponseEntity.ok(response);
    }
}
