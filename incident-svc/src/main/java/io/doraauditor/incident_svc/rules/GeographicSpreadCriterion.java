package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 7: Geographical spread.
 * Threshold: Services or entities affected across 2 or more EU Member States.
 */
@Component
@Order(7)
public class GeographicSpreadCriterion implements RegulatoryCriterion {

    public static final int GEOGRAPHIC_SPREAD_THRESHOLD = 2;

    @Override
    public String criterionCode() {
        return "CROSS_BORDER_GEOGRAPHIC_SPREAD";
    }

    @Override
    public String description() {
        return "Incident impacts services in 2 or more EU Member States";
    }

    @Override
    public boolean isTriggered(IncidentRequest request) {
        return request.geographicSpreadCount() != null
                && request.geographicSpreadCount() >= GEOGRAPHIC_SPREAD_THRESHOLD;
    }
}
