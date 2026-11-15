package io.doraauditor.incident_svc.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an ICT incident record in the {@code incident.incident} database table.
 * <p>
 * Implements optimistic concurrency control via a {@code @Version} column to guard against lost updates
 * during concurrent compliance workflows. Contains audit timestamps, detection/awareness moments,
 * statutory classification deadlines, and FSM lifecycle status.
 * </p>
 */
@Entity
@Table(name = "incident", schema = "incident", uniqueConstraints = {
        @UniqueConstraint(name = "uq_incident_source_event", columnNames = {"source", "source_event_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Column(name = "source_event_id", nullable = false, length = 128)
    private String sourceEventId;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "awareness_at", nullable = false)
    private Instant awarenessAt;

    @Column(name = "classified_at")
    private Instant classifiedAt;

    @Column(name = "classification_deadline")
    private Instant classificationDeadline;

    @Column(name = "is_major")
    private Boolean isMajor;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "service_id", nullable = false, length = 128)
    private String serviceId;

    @Column(name = "severity_hint", length = 16)
    private String severityHint;

    @Column(name = "raw_logs_ref", length = 512)
    private String rawLogsRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IncidentStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * JPA lifecycle callback executed prior to an entity update to synchronize the modification timestamp.
     */
    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions this incident to a new lifecycle status, validating the transition against the FSM matrix.
     *
     * @param nextStatus the proposed next {@link IncidentStatus}
     * @throws IllegalStateException if the requested transition is prohibited by {@link IncidentStatus#canTransitionTo}
     */
    public void transitionTo(IncidentStatus nextStatus) {
        if (!this.status.canTransitionTo(nextStatus)) {
            throw new IllegalStateException("Illegal state transition from " + this.status + " to " + nextStatus);
        }
        this.status = nextStatus;
    }
}
