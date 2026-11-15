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

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findBySourceAndSourceEventId(String source, String sourceEventId);

    @Query("SELECT i FROM Incident i WHERE (:status IS NULL OR i.status = :status) AND (:isMajor IS NULL OR i.isMajor = :isMajor)")
    Page<Incident> findByFilters(
            @Param("status") IncidentStatus status,
            @Param("isMajor") Boolean isMajor,
            Pageable pageable
    );
}
