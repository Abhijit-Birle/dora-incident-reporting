package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 6: Reputational impact.
 * <p>
 * Triggered by external visibility, media reporting, or regulatory/client complaints that damage
 * the financial entity's public trust or brand standing.
 * </p>
 */
@Component
@Order(6)
public class ReputationalImpactCriterion implements RegulatoryCriterion {

    /**
     * {@inheritDoc}
     */
    @Override
    public String criterionCode() {
        return "REPUTATIONAL_IMPACT_REPORTED";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String description() {
        return "Incident caused external media exposure or supervisory reputational harm";
    }

    /**
     * Evaluates whether reputational damage was incurred as a result of the incident.
     *
     * @param request the incident intake request
     * @return {@code true} if {@link IncidentRequest#reputationalImpact()} is {@code Boolean.TRUE}
     */
    @Override
    public boolean isTriggered(IncidentRequest request) {
        return Boolean.TRUE.equals(request.reputationalImpact());
    }
}
