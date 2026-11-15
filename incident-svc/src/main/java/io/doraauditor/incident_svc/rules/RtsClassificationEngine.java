package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite evaluator implementing the major incident classification formula under
 * Commission Delegated Regulation (EU) 2024/1772 (RTS) Article 9.
 *
 * Formula:
 * An incident is classified as MAJOR if:
 * 1. Critical service is affected (Art. 1 gatekeeper), AND
 * 2. EITHER:
 *    a) Data integrity / data loss is compromised (Art. 5), OR
 *    b) At least 2 materiality threshold criteria are met (Arts. 2, 3, 4, 6, 7).
 */
@Component
public class RtsClassificationEngine {

    private final List<RegulatoryCriterion> criteria;

    /**
     * Constructs an {@code RtsClassificationEngine} with the ordered list of regulatory criteria beans.
     *
     * @param criteria list of Spring-managed {@link RegulatoryCriterion} component implementations
     */
    public RtsClassificationEngine(List<RegulatoryCriterion> criteria) {
        this.criteria = criteria;
    }

    /**
     * Evaluates an incoming incident request against all registered RTS criteria using the Article 9 composite logic.
     * <p>
     * An incident is classified as MAJOR if and only if:
     * <ol>
     *   <li>A critical or important function is affected (Article 1 gatekeeper), AND</li>
     *   <li>Either:
     *     <ul>
     *       <li>Data integrity, confidentiality, or authenticity is compromised (Article 5), OR</li>
     *       <li>At least 2 secondary materiality thresholds are met (Articles 2, 3, 4, 6, 7).</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     *
     * @param request the incident intake request containing telemetry, impact figures, and criticality flags
     * @return {@link ClassificationResult} detailing whether the incident is major, the rationale, and criteria met
     */
    public ClassificationResult evaluate(IncidentRequest request) {
        List<String> criteriaMet = new ArrayList<>();
        int materialityCount = 0;
        boolean criticalServiceAffected = false;
        boolean dataIntegrityImpacted = false;

        for (RegulatoryCriterion criterion : criteria) {
            if (criterion.isTriggered(request)) {
                criteriaMet.add(criterion.criterionCode());

                if (criterion instanceof CriticalServiceCriterion) {
                    criticalServiceAffected = true;
                } else if (criterion instanceof DataIntegrityCriterion) {
                    dataIntegrityImpacted = true;
                    materialityCount++;
                } else if (criterion.isMaterialityThreshold()) {
                    materialityCount++;
                }
            }
        }

        // Decision logic under RTS 2024/1772
        if (!criticalServiceAffected) {
            return new ClassificationResult(
                    false,
                    "Classified as NON-MAJOR: Affected service is not designated as a critical or important function.",
                    criteriaMet
            );
        }

        if (dataIntegrityImpacted) {
            return new ClassificationResult(
                    true,
                    "Classified as MAJOR under DORA RTS Art. 9: Critical service affected with data loss/integrity compromise.",
                    criteriaMet
            );
        }

        if (materialityCount >= 2) {
            return new ClassificationResult(
                    true,
                    String.format("Classified as MAJOR under DORA RTS Art. 9: Critical service affected with %d materiality thresholds met.", materialityCount),
                    criteriaMet
            );
        }

        return new ClassificationResult(
                false,
                String.format("Classified as NON-MAJOR: Critical service affected but only %d of required 2 materiality thresholds met.", materialityCount),
                criteriaMet
        );
    }
}
