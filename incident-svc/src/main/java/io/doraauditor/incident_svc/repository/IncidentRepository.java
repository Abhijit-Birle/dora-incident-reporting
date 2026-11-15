package io.doraauditor.incident_svc.repository;

import io.doraauditor.incident_svc.domain.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    Optional<Incident> findBySourceAndSourceEventId(String source, String sourceEventId);
}
