package io.doraauditor.incident_svc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity representing a cached idempotency key entry in the {@code incident.idempotency_key} table.
 * <p>
 * Stores the SHA-256 fingerprint of the original HTTP request payload along with the cached response JSON.
 * Configured with a 24-hour expiration window to support safe client retries without duplicate processing.
 * </p>
 */
@Entity
@Table(name = "idempotency_key", schema = "incident")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKeyRecord {

    @Id
    @Column(name = "key", length = 128, nullable = false)
    private String key;

    @Column(name = "request_hash", length = 80, nullable = false)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Checks whether this idempotency cache record has passed its retention TTL.
     *
     * @return {@code true} if the current instant is strictly after {@link #getExpiresAt()}, {@code false} otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}
