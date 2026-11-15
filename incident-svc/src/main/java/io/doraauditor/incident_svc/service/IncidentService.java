package io.doraauditor.incident_svc.service;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import io.doraauditor.incident_svc.dto.IncidentResponse;

public interface IncidentService {
    IncidentResponse processIncident(String idempotencyKey, IncidentRequest request);
}
