package io.doraauditor.incident_svc.service.impl;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import io.doraauditor.incident_svc.dto.IncidentResponse;
import io.doraauditor.incident_svc.service.IncidentService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class IncidentServiceImpl implements IncidentService {

    @Override
    public IncidentResponse processIncident(String idempotencyKey, IncidentRequest request) {
        Instant now = Instant.now();
        Instant deadline = request.awarenessAt().plusSeconds(24 * 3600);

        return new IncidentResponse(
                UUID.randomUUID(),
                request.source(),
                request.sourceEventId(),
                request.title(),
                request.serviceId(),
                "RECEIVED",
                0L,
                false,
                "Incident ingested",
                List.of(),
                request.detectedAt(),
                request.awarenessAt(),
                now,
                deadline,
                1440L,
                now,
                false
        );
    }
}
