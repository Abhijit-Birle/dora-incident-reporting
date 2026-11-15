package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;

/**
 * Specification contract for evaluating individual RTS (EU 2024/1772) regulatory criteria.
 */
public interface RegulatoryCriterion {

    /**
     * Unique regulatory criterion code (e.g., "CRITICAL_SERVICE_AFFECTED").
     */
    String criterionCode();

    /**
     * Legal reference and description of the threshold condition.
     */
    String description();

    /**
     * Evaluates whether the incoming incident request satisfies this criterion.
     */
    boolean isTriggered(IncidentRequest request);

    /**
     * Whether this criterion is considered a "materiality threshold" that contributes to the 2+ threshold rule.
     */
    default boolean isMaterialityThreshold() {
        return true;
    }
}
