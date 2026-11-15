package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 6: Reputational impact.
 * Triggered by external visibility, media reporting, or regulatory/client complaints.
 */
@Component
@Order(6)
public class ReputationalImpactCriterion implements RegulatoryCriterion {

    @Override
    public String criterionCode() {
        return "REPUTATIONAL_IMPACT_REPORTED";
    }

    @Override
    public String description() {
        return "Incident caused external media exposure or supervisory reputational harm";
    }

    @Override
    public boolean isTriggered(IncidentRequest request) {
        return Boolean.TRUE.equals(request.reputationalImpact());
    }
}
