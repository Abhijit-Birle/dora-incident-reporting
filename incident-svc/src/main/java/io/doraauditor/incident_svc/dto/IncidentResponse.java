package io.doraauditor.incident_svc.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response returned after incident intake and deterministic classification.
 */
public record IncidentResponse(
        // 1. Identification & Correlation
        UUID incidentId,
        String source,
        String sourceEventId,
        String title,
        String serviceId,

        // 2. Lifecycle & Concurrency
        String status,
        Long version,

        // 3. RTS (EU 2024/1772) Classification Result
        Boolean isMajor,
        String classificationReason,
        List<String> criteriaMet,

        // 4. Statutory Dual-Clock Deadlines (DORA Art. 19 & APRA CPS 230)
        Instant detectedAt,
        Instant awarenessAt,
        Instant classifiedAt,
        Instant classificationDeadline,
        Long deadlineCountdownMinutes,

        // 5. Audit & Idempotency Metadata
        Instant createdAt,
        boolean idempotentReplay
) {}