package io.doraauditor.incident_svc.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response model returned after incident intake, deterministic classification, or state machine transitions.
 *
 * @param incidentId               unique persistent UUID of the incident
 * @param source                   originating observability system (e.g. "DATADOG")
 * @param sourceEventId            external event ID from the source system
 * @param title                    short summary of the incident
 * @param serviceId                impacted service or infrastructure component identifier
 * @param status                   current FSM lifecycle status (e.g. "CLASSIFIED", "DRAFTING", "APPROVED")
 * @param version                  optimistic concurrency version token for safe updates
 * @param isMajor                  statutory major incident classification decision under RTS Article 9
 * @param classificationReason     legal rationale detailing why the incident was classified as major or non-major
 * @param criteriaMet              list of specific RTS regulatory criterion codes that were triggered
 * @param detectedAt               timestamp when the technical failure first manifested
 * @param awarenessAt              timestamp when the financial entity became aware
 * @param classifiedAt             timestamp when the classification engine completed evaluation
 * @param classificationDeadline   earliest statutory submission cutoff {@code min(classifiedAt + 4h, awarenessAt + 24h)}
 * @param deadlineCountdownMinutes dynamic remaining minutes until the statutory deadline cutoff
 * @param createdAt                audit timestamp when this entity record was persisted
 * @param idempotentReplay         {@code true} if this response was served from the idempotency cache replay
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