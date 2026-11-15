package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 2: Clients, financial counterparts, and transactions affected.
 * <p>
 * Triggered when the number of affected clients exceeds 100,000 OR exceeds 10% of the financial entity's
 * total active client base for the impacted service.
 * </p>
 */
@Component
@Order(2)
public class AffectedClientsCriterion implements RegulatoryCriterion {

    /** Absolute threshold: 100,000 affected clients. */
    public static final long ABSOLUTE_CLIENT_THRESHOLD = 100_000L;

    /** Percentage threshold: 10.0% of active client base. */
    public static final double PERCENTAGE_CLIENT_THRESHOLD = 10.0;

    /**
     * {@inheritDoc}
     */
    @Override
    public String criterionCode() {
        return "CLIENTS_AFFECTED_THRESHOLD_EXCEEDED";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String description() {
        return "Affected clients exceed 100,000 or 10% of active client base";
    }

    /**
     * Evaluates whether the affected client count or percentage breaches the RTS Article 2 thresholds.
     *
     * @param request the incident intake request containing client metrics
     * @return {@code true} if either absolute count &gt; 100,000 or percentage &gt; 10.0%, {@code false} otherwise
     */
    @Override
    public boolean isTriggered(IncidentRequest request) {
        boolean absoluteExceeded = request.affectedClientsCount() != null
                && request.affectedClientsCount() > ABSOLUTE_CLIENT_THRESHOLD;

        boolean percentageExceeded = request.affectedClientsPercentage() != null
                && request.affectedClientsPercentage() > PERCENTAGE_CLIENT_THRESHOLD;

        return absoluteExceeded || percentageExceeded;
    }
}
