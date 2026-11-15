package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 1: Criticality of the services affected.
 * Determines if the incident affects critical or important functions of the financial entity.
 */
@Component
@Order(1)
public class CriticalServiceCriterion implements RegulatoryCriterion {

    @Override
    public String criterionCode() {
        return "CRITICAL_SERVICE_AFFECTED";
    }

    @Override
    public String description() {
        return "Critical or important function of the financial entity is impacted";
    }

    @Override
    public boolean isTriggered(IncidentRequest request) {
        return Boolean.TRUE.equals(request.isCriticalService());
    }

    @Override
    public boolean isMaterialityThreshold() {
        return false; // Gatekeeper criterion, not one of the secondary materiality thresholds
    }
}
