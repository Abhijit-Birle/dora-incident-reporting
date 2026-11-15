package io.doraauditor.incident_svc.controller;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import io.doraauditor.incident_svc.dto.IncidentResponse;
import io.doraauditor.incident_svc.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping("incident")
    public ResponseEntity<IncidentResponse> createIncident(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody IncidentRequest request) {

        // 1. If client didn't supply a key, generate a fallback from domain fields
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = request.source() + ":" + request.sourceEventId();
        }

        // 2. Delegate to IncidentService (checks DB, hashes payload, returns cached or processes)
        IncidentResponse response = incidentService.processIncident(idempotencyKey, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
