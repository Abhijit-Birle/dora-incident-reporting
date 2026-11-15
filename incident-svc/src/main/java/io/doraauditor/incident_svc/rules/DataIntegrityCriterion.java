package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 5: Data losses and integrity impact.
 * <p>
 * Triggered when data confidentiality, integrity, or authenticity is compromised.
 * Under RTS Article 9, if a critical service is affected and data integrity is compromised,
 * the incident is automatically classified as MAJOR without requiring additional materiality thresholds.
 * </p>
 */
@Component
@Order(5)
public class DataIntegrityCriterion implements RegulatoryCriterion {

    /**
     * {@inheritDoc}
     */
    @Override
    public String criterionCode() {
        return "DATA_INTEGRITY_COMPROMISED";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String description() {
        return "Data loss, corruption, or unauthorized alteration of records occurred";
    }

    /**
     * Evaluates whether data integrity or confidentiality was impacted.
     *
     * @param request the incident intake request
     * @return {@code true} if {@link IncidentRequest#dataIntegrityImpacted()} is {@code Boolean.TRUE}
     */
    @Override
    public boolean isTriggered(IncidentRequest request) {
        return Boolean.TRUE.equals(request.dataIntegrityImpacted());
    }
}
