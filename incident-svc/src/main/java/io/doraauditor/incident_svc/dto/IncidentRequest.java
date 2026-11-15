package io.doraauditor.incident_svc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

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
    // Compact constructor to set sensible defaults
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