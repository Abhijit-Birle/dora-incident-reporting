package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;

/**
 * Specification contract for evaluating individual regulatory criteria under Commission Delegated
 * Regulation (EU) 2024/1772 (Regulatory Technical Standards on major incident classification).
 * <p>
 * Implements the Specification / Composite design pattern, allowing each regulatory article to encapsulate
 * its legal thresholds, units of measure, and evaluation logic independently.
 * </p>
 */
public interface RegulatoryCriterion {

    /**
     * Returns the unique machine-readable criterion code identifying this rule in audit trails and reports.
     *
     * @return uppercase criterion code string (e.g. {@code "CRITICAL_SERVICE_AFFECTED"})
     */
    String criterionCode();

    /**
     * Returns the regulatory description and statutory citation of this threshold condition.
     *
     * @return human-readable description citing the relevant RTS article
     */
    String description();

    /**
     * Evaluates whether the incoming incident intake request satisfies or exceeds this regulatory threshold.
     *
     * @param request the incident intake request containing telemetry and operational metrics
     * @return {@code true} if the criterion is satisfied or breached, {@code false} otherwise
     */
    boolean isTriggered(IncidentRequest request);

    /**
     * Indicates whether this criterion qualifies as one of the secondary "materiality thresholds"
     * under RTS (EU) 2024/1772 Article 9.
     * <p>
     * Under Article 9, an incident impacting a critical function requires either data integrity compromise
     * OR at least two materiality thresholds to be met to trigger major incident classification.
     * Non-materiality gatekeeper criteria (such as {@link CriticalServiceCriterion}) return {@code false}.
     * </p>
     *
     * @return {@code true} if this rule counts toward the 2+ materiality threshold count, {@code false} if gatekeeper
     */
    default boolean isMaterialityThreshold() {
        return true;
    }
}
