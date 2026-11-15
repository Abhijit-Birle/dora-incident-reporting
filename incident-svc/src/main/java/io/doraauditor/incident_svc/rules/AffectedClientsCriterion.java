package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 2: Clients and financial counterparties affected.
 * Threshold: > 100,000 clients affected OR > 10% of total active client base.
 */
@Component
@Order(2)
public class AffectedClientsCriterion implements RegulatoryCriterion {

    public static final long ABSOLUTE_CLIENT_THRESHOLD = 100_000L;
    public static final double PERCENTAGE_CLIENT_THRESHOLD = 10.0;

    @Override
    public String criterionCode() {
        return "CLIENTS_AFFECTED_THRESHOLD_EXCEEDED";
    }

    @Override
    public String description() {
        return "Affected clients exceed 100,000 or 10% of active client base";
    }

    @Override
    public boolean isTriggered(IncidentRequest request) {
        boolean absoluteExceeded = request.affectedClientsCount() != null
                && request.affectedClientsCount() > ABSOLUTE_CLIENT_THRESHOLD;

        boolean percentageExceeded = request.affectedClientsPercentage() != null
                && request.affectedClientsPercentage() > PERCENTAGE_CLIENT_THRESHOLD;

        return absoluteExceeded || percentageExceeded;
    }
}
