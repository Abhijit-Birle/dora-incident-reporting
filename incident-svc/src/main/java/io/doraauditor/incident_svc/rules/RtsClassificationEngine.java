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

    public RtsClassificationEngine(List<RegulatoryCriterion> criteria) {
        this.criteria = criteria;
    }

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
