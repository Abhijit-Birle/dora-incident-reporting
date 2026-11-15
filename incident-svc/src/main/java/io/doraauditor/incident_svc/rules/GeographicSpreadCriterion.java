package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 7: Geographical spread.
 * <p>
 * Triggered when the incident impacts operations, clients, or market infrastructures
 * across 2 or more EU Member States.
 * </p>
 */
@Component
@Order(7)
public class GeographicSpreadCriterion implements RegulatoryCriterion {

    /** Regulatory cross-border threshold: 2 EU Member States. */
    public static final int GEOGRAPHIC_SPREAD_THRESHOLD = 2;

    /**
     * {@inheritDoc}
     */
    @Override
    public String criterionCode() {
        return "CROSS_BORDER_GEOGRAPHIC_SPREAD";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String description() {
        return "Incident impacts services in 2 or more EU Member States";
    }

    /**
     * Evaluates whether the cross-border geographic spread threshold is breached.
     *
     * @param request the incident intake request containing geographic spread count
     * @return {@code true} if {@link IncidentRequest#geographicSpreadCount()} &ge; 2
     */
    @Override
    public boolean isTriggered(IncidentRequest request) {
        return request.geographicSpreadCount() != null
                && request.geographicSpreadCount() >= GEOGRAPHIC_SPREAD_THRESHOLD;
    }
}
