package io.doraauditor.incident_svc.repository;

import io.doraauditor.incident_svc.domain.Incident;
import io.doraauditor.incident_svc.domain.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Incident} entity persistence and query operations.
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    /**
     * Finds an incident by originating system source and external event identifier.
     * <p>
     * Used as a secondary deduplication safeguard to prevent duplicate entity records when clients
     * submit the same alert from monitoring systems without an HTTP idempotency key header.
     * </p>
     *
     * @param source        the source system identifier (e.g. "DATADOG", "PAGERDUTY")
     * @param sourceEventId the event ID assigned by the source monitoring platform
     * @return an {@link Optional} containing the matching {@link Incident}, or empty if not found
     */
    Optional<Incident> findBySourceAndSourceEventId(String source, String sourceEventId);

    /**
     * Executes a dynamic query filtering incidents by optional lifecycle status and major qualification with pagination.
     *
     * @param status   optional filter matching {@link Incident#getStatus()} (omitted if null)
     * @param isMajor  optional filter matching {@link Incident#getIsMajor()} (omitted if null)
     * @param pageable pagination parameters including page size, offset, and sort order
     * @return a {@link Page} of matching {@link Incident} entities
     */
    @Query("SELECT i FROM Incident i WHERE (:status IS NULL OR i.status = :status) AND (:isMajor IS NULL OR i.isMajor = :isMajor)")
    Page<Incident> findByFilters(
            @Param("status") IncidentStatus status,
            @Param("isMajor") Boolean isMajor,
            Pageable pageable
    );
}
