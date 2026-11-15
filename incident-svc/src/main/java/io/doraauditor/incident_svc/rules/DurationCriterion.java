package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 4: Duration and downtime.
 * <p>
 * Triggered when service downtime or severe degradation of an ICT service exceeds 2 hours (120 minutes).
 * </p>
 */
@Component
@Order(4)
public class DurationCriterion implements RegulatoryCriterion {

    /** Regulatory downtime threshold: 120 minutes (2 hours). */
    public static final long DOWNTIME_MINUTES_THRESHOLD = 120L;

    /**
     * {@inheritDoc}
     */
    @Override
    public String criterionCode() {
        return "DOWNTIME_EXCEEDS_2_HOURS";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String description() {
        return "Downtime or severe service degradation exceeds 120 minutes";
    }

    /**
     * Evaluates whether service disruption duration exceeds the 2-hour threshold.
     *
     * @param request the incident intake request containing duration metrics
     * @return {@code true} if {@link IncidentRequest#durationMinutes()} is strictly greater than 120 minutes
     */
    @Override
    public boolean isTriggered(IncidentRequest request) {
        return request.durationMinutes() != null
                && request.durationMinutes() > DOWNTIME_MINUTES_THRESHOLD;
    }
}
