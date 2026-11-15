package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * RTS (EU 2024/1772) Article 3: Economic impact.
 * <p>
 * Triggered when direct financial losses, remediation costs, or transactional values impacted
 * exceed the statutory materiality threshold of &euro;100,000.
 * </p>
 */
@Component
@Order(3)
public class EconomicImpactCriterion implements RegulatoryCriterion {

    /** Regulatory threshold: €100,000.00 direct economic loss. */
    public static final BigDecimal ECONOMIC_THRESHOLD_EUR = new BigDecimal("100000.00");

    /**
     * {@inheritDoc}
     */
    @Override
    public String criterionCode() {
        return "ECONOMIC_IMPACT_EXCEEDS_100K";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String description() {
        return "Direct financial impact or transaction volume exceeds €100,000";
    }

    /**
     * Evaluates whether direct economic losses exceed the &euro;100,000 statutory limit.
     *
     * @param request the incident intake request containing financial loss metrics
     * @return {@code true} if {@link IncidentRequest#economicImpactAmount()} is strictly greater than &euro;100,000.00
     */
    @Override
    public boolean isTriggered(IncidentRequest request) {
        return request.economicImpactAmount() != null
                && request.economicImpactAmount().compareTo(ECONOMIC_THRESHOLD_EUR) > 0;
    }
}
