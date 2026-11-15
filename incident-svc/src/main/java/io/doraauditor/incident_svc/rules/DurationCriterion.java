package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RTS (EU 2024/1772) Article 4: Duration and downtime.
 * Threshold: Service downtime or severe degradation exceeding 2 hours (120 minutes).
 */
@Component
@Order(4)
public class DurationCriterion implements RegulatoryCriterion {

    public static final long DOWNTIME_MINUTES_THRESHOLD = 120L;

    @Override
    public String criterionCode() {
        return "DOWNTIME_EXCEEDS_2_HOURS";
    }

    @Override
    public String description() {
        return "Downtime or severe service degradation exceeds 120 minutes";
    }

    @Override
    public boolean isTriggered(IncidentRequest request) {
        return request.durationMinutes() != null
                && request.durationMinutes() > DOWNTIME_MINUTES_THRESHOLD;
    }
}
