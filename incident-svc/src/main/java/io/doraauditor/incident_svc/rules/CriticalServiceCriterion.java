package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 1: Criticality of the services affected.
 * <p>
 * Evaluates whether the ICT incident impacts a critical or important function (CIF) of the financial entity.
 * Serves as the mandatory primary gatekeeper: under RTS Article 9, an incident cannot be classified as MAJOR
 * unless a critical function is impacted.
 * </p>
 */
@Component
@Order(1)
public class CriticalServiceCriterion implements RegulatoryCriterion {

    /**
     * {@inheritDoc}
     */
    @Override
    public String criterionCode() {
        return "CRITICAL_SERVICE_AFFECTED";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String description() {
        return "Critical or important function of the financial entity is impacted";
    }

    /**
     * Determines whether the requested incident affects a critical service function.
     *
     * @param request the incoming incident intake request
     * @return {@code true} if {@link IncidentRequest#isCriticalService()} is {@code Boolean.TRUE}, {@code false} otherwise
     */
    @Override
    public boolean isTriggered(IncidentRequest request) {
        return Boolean.TRUE.equals(request.isCriticalService());
    }

    /**
     * Returns {@code false} because critical service criticality acts as a prerequisite gatekeeper,
     * not as a secondary materiality threshold under RTS Article 9.
     *
     * @return {@code false}
     */
    @Override
    public boolean isMaterialityThreshold() {
        return false;
    }
}
