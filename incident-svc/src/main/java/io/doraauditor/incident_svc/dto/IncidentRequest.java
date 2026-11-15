package io.doraauditor.incident_svc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Incoming incident alert intake payload containing monitoring telemetry, operational scope,
 * and RTS (EU 2024/1772) quantitative and qualitative regulatory metrics.
 *
 * @param source                   originating monitoring system (e.g. "DATADOG", "PAGERDUTY", "PROMETHEUS")
 * @param sourceEventId            external unique event ID in the originating platform
 * @param detectedAt               timestamp when the technical failure first manifested
 * @param awarenessAt              timestamp when the financial entity became aware (starts DORA Art. 19 24h clock)
 * @param title                    short summary of the incident
 * @param description              detailed operational breakdown or incident log excerpt
 * @param serviceId                impacted service or infrastructure component identifier
 * @param severityHint             originating severity rating (e.g. "P1", "CRITICAL")
 * @param rawLogsRef               pointer to raw diagnostic logs (e.g. S3 URI or APM trace link)
 * @param isCriticalService        RTS Art. 1: whether the affected component supports a Critical or Important Function (CIF)
 * @param affectedClientsCount     RTS Art. 2: absolute count of impacted clients (threshold &gt; 100,000)
 * @param affectedClientsPercentage RTS Art. 2: percentage of active client base impacted (threshold &gt; 10%)
 * @param durationMinutes          RTS Art. 4: cumulative downtime or service degradation in minutes (threshold &gt; 120m)
 * @param economicImpactAmount     RTS Art. 3: direct financial loss or impacted transaction volume in EUR (threshold &gt; &euro;100k)
 * @param dataIntegrityImpacted    RTS Art. 5: whether data confidentiality, integrity, or authenticity was compromised
 * @param reputationalImpact       RTS Art. 6: whether the event caused public media exposure or supervisory complaints
 * @param geographicSpreadCount    RTS Art. 7: count of impacted EU Member States (threshold &ge; 2)
 */
public record IncidentRequest(
        // -------------------------------------------------------------
        // 1. Source System & Tracking Metadata
        // -------------------------------------------------------------
        @NotBlank(message = "Source is required (e.g., DATADOG, PAGERDUTY)")
        @Size(max = 64)
        String source,

        @NotBlank(message = "Source event ID is required")
        @Size(max = 128)
        String sourceEventId,

        @NotNull(message = "Detected timestamp is required")
        Instant detectedAt,

        @NotNull(message = "Awareness timestamp is required (starts the DORA 24h statutory clock)")
        Instant awarenessAt,

        // -------------------------------------------------------------
        // 2. Incident Description & Scope
        // -------------------------------------------------------------
        @NotBlank(message = "Title is required")
        @Size(max = 256)
        String title,

        String description,

        @NotBlank(message = "Service ID is required")
        @Size(max = 128)
        String serviceId,

        @Size(max = 16)
        String severityHint, // e.g. "P1", "CRITICAL", "HIGH"

        @Size(max = 512)
        String rawLogsRef,   // e.g. S3 URI or Datadog trace link

        // -------------------------------------------------------------
        // 3. RTS (EU 2024/1772) Regulatory Classification Metrics
        // -------------------------------------------------------------
        @NotNull(message = "Critical service flag is required")
        Boolean isCriticalService, // RTS Art. 1: Critical or important function?

        @PositiveOrZero(message = "Affected clients count cannot be negative")
        Long affectedClientsCount, // RTS threshold: > 100,000 clients

        Double affectedClientsPercentage, // RTS threshold: > 10% of total clients

        @PositiveOrZero(message = "Duration in minutes cannot be negative")
        Long durationMinutes, // RTS threshold: > 2 hours (120 min) downtime

        BigDecimal economicImpactAmount, // RTS threshold: > €100,000

        Boolean dataIntegrityImpacted, // RTS: Data loss / corruption / fraud

        Boolean reputationalImpact, // RTS: Media coverage / regulatory complaints

        @PositiveOrZero(message = "Geographic spread cannot be negative")
        Integer geographicSpreadCount // RTS threshold: >= 2 EU Member States
) {
    /**
     * Compact constructor enforcing regulatory defaults and defensive field sanitization.
     * <p>
     * Defaults {@code awarenessAt} to {@code detectedAt} if omitted, defaults boolean metrics
     * ({@code dataIntegrityImpacted}, {@code reputationalImpact}) to {@code false}, and sets
     * minimum geographic spread to 1 Member State.
     * </p>
     */
    public IncidentRequest {
        if (awarenessAt == null && detectedAt != null) {
            awarenessAt = detectedAt;
        }
        if (dataIntegrityImpacted == null) {
            dataIntegrityImpacted = false;
        }
        if (reputationalImpact == null) {
            reputationalImpact = false;
        }
        if (geographicSpreadCount == null) {
            geographicSpreadCount = 1;
        }
    }
}