package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * RTS (EU 2024/1772) Article 3: Economic impact.
 * Threshold: Direct economic losses or transaction impact exceeding €100,000.
 */
@Component
@Order(3)
public class EconomicImpactCriterion implements RegulatoryCriterion {

    public static final BigDecimal ECONOMIC_THRESHOLD_EUR = new BigDecimal("100000.00");

    @Override
    public String criterionCode() {
        return "ECONOMIC_IMPACT_EXCEEDS_100K";
    }

    @Override
    public String description() {
        return "Direct financial impact or transaction volume exceeds €100,000";
    }

    @Override
    public boolean isTriggered(IncidentRequest request) {
        return request.economicImpactAmount() != null
                && request.economicImpactAmount().compareTo(ECONOMIC_THRESHOLD_EUR) > 0;
    }
}
