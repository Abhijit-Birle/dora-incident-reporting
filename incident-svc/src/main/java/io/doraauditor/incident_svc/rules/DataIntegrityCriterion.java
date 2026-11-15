package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 5: Data losses and integrity impact.
 * Triggered when data confidentiality, integrity, or authenticity is compromised.
 */
@Component
@Order(5)
public class DataIntegrityCriterion implements RegulatoryCriterion {

    @Override
    public String criterionCode() {
        return "DATA_INTEGRITY_COMPROMISED";
    }

    @Override
    public String description() {
        return "Data loss, corruption, or unauthorized alteration of records occurred";
    }

    @Override
    public boolean isTriggered(IncidentRequest request) {
        return Boolean.TRUE.equals(request.dataIntegrityImpacted());
    }
}
