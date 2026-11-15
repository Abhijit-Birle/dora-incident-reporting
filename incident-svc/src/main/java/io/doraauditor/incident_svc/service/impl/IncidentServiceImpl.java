package io.doraauditor.incident_svc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.doraauditor.incident_svc.domain.IdempotencyKeyRecord;
import io.doraauditor.incident_svc.domain.Incident;
import io.doraauditor.incident_svc.domain.IncidentStatus;
import io.doraauditor.incident_svc.dto.IncidentRequest;
import io.doraauditor.incident_svc.dto.IncidentResponse;
import io.doraauditor.incident_svc.dto.TransitionStatusRequest;
import io.doraauditor.incident_svc.exception.IdempotencyConflictException;
import io.doraauditor.incident_svc.exception.IncidentNotFoundException;
import io.doraauditor.incident_svc.repository.IdempotencyKeyRepository;
import io.doraauditor.incident_svc.repository.IncidentRepository;
import io.doraauditor.incident_svc.rules.ClassificationResult;
import io.doraauditor.incident_svc.rules.RtsClassificationEngine;
import io.doraauditor.incident_svc.service.IncidentService;
import io.doraauditor.incident_svc.util.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final RtsClassificationEngine classificationEngine;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public IncidentResponse processIncident(String idempotencyKey, IncidentRequest request) {
        String currentHash = computePayloadHash(request);

        // -------------------------------------------------------------
        // Step 1: Idempotency Key & Replay Verification
        // -------------------------------------------------------------
        Optional<IdempotencyKeyRecord> existingKey = idempotencyKeyRepository.findById(idempotencyKey);
        if (existingKey.isPresent()) {
            IdempotencyKeyRecord record = existingKey.get();

            // Detect mismatched payload replay
            if (!record.getRequestHash().equals(currentHash)) {
                log.warn("Idempotency conflict detected for key: {}. Hashes do not match.", idempotencyKey);
                throw new IdempotencyConflictException(
                        "Idempotency key '" + idempotencyKey + "' was previously used with a different request payload."
                );
            }

            // Return cached response (mark as replay)
            log.info("Idempotent replay detected for key: {}. Serving cached response.", idempotencyKey);
            return deserializeCachedResponse(record.getResponseBody());
        }

        // -------------------------------------------------------------
        // Step 2: Source Event Deduplication (Secondary Safety Check)
        // -------------------------------------------------------------
        Optional<Incident> existingIncident = incidentRepository.findBySourceAndSourceEventId(
                request.source(), request.sourceEventId()
        );
        if (existingIncident.isPresent()) {
            Incident incident = existingIncident.get();
            log.info("Incident already recorded for source: {} and eventId: {}", request.source(), request.sourceEventId());
            return buildResponseFromEntity(incident, "Previously classified incident", true);
        }

        // -------------------------------------------------------------
        // Step 3: RTS (EU 2024/1772) Deterministic Rules Evaluation
        // -------------------------------------------------------------
        ClassificationResult classification = classificationEngine.evaluate(request);
        log.info("Incident classification complete: isMajor={}, reason='{}'",
                classification.isMajor(), classification.reason());

        // -------------------------------------------------------------
        // Step 4: Statutory Dual-Clock Deadline Calculation (DORA Art. 19)
        // -------------------------------------------------------------
        Instant now = Instant.now();
        Instant awarenessAt = request.awarenessAt();
        Instant classifiedAt = now;
        Instant deadline = null;
        Long countdownMinutes = null;

        if (classification.isMajor()) {
            // Clock 1: Initial notification due <= 4 hours from classification
            Instant clock1_4h = classifiedAt.plus(Duration.ofHours(4));

            // Clock 2: Must be submitted <= 24 hours from initial awareness
            Instant clock2_24h = awarenessAt.plus(Duration.ofHours(24));

            // Strictest (earliest) statutory cutoff applies
            deadline = clock1_4h.isBefore(clock2_24h) ? clock1_4h : clock2_24h;
            countdownMinutes = Math.max(0, Duration.between(now, deadline).toMinutes());

            log.info("Statutory 4h/24h clocks armed: deadline={}, countdownMinutes={}", deadline, countdownMinutes);
        }

        // -------------------------------------------------------------
        // Step 5: Persist Incident Entity (FSM Initial State & @Version)
        // -------------------------------------------------------------
        Incident incident = Incident.builder()
                .incidentId(UUID.randomUUID())
                .source(request.source())
                .sourceEventId(request.sourceEventId())
                .detectedAt(request.detectedAt())
                .awarenessAt(awarenessAt)
                .classifiedAt(classifiedAt)
                .classificationDeadline(deadline)
                .isMajor(classification.isMajor())
                .title(request.title())
                .description(request.description())
                .serviceId(request.serviceId())
                .severityHint(request.severityHint())
                .rawLogsRef(request.rawLogsRef())
                .status(IncidentStatus.CLASSIFIED)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        incident = incidentRepository.save(incident);
        log.info("Persisted incident entity with ID: {}", incident.getIncidentId());

        // -------------------------------------------------------------
        // Step 6: Cache Response in incident.idempotency_key Table
        // -------------------------------------------------------------
        IncidentResponse response = new IncidentResponse(
                incident.getIncidentId(),
                incident.getSource(),
                incident.getSourceEventId(),
                incident.getTitle(),
                incident.getServiceId(),
                incident.getStatus().name(),
                incident.getVersion(),
                incident.getIsMajor(),
                classification.reason(),
                classification.criteriaMet(),
                incident.getDetectedAt(),
                incident.getAwarenessAt(),
                incident.getClassifiedAt(),
                incident.getClassificationDeadline(),
                countdownMinutes,
                incident.getCreatedAt(),
                false // fresh intake
        );

        saveIdempotencyRecord(idempotencyKey, currentHash, response, now);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getIncident(UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        return buildResponseFromEntity(incident, "Incident status: " + incident.getStatus(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> listIncidents(IncidentStatus status, Boolean isMajor, Pageable pageable) {
        Page<Incident> page = incidentRepository.findByFilters(status, isMajor, pageable);
        return page.map(incident -> buildResponseFromEntity(incident, "Incident status: " + incident.getStatus(), false));
    }

    @Override
    @Transactional
    public IncidentResponse transitionStatus(UUID incidentId, TransitionStatusRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        // Optimistic locking check
        if (!incident.getVersion().equals(request.version())) {
            log.warn("Optimistic lock conflict on incident {}. Provided version: {}, current: {}",
                    incidentId, request.version(), incident.getVersion());
            throw new OptimisticLockingFailureException(
                    "Incident version conflict. Provided version: " + request.version() + ", current version: " + incident.getVersion()
            );
        }

        IncidentStatus previousStatus = incident.getStatus();
        incident.transitionTo(request.targetStatus());
        incident = incidentRepository.save(incident);

        log.info("Incident {} transitioned successfully: {} -> {} by actor: '{}' (reason: '{}')",
                incidentId, previousStatus, request.targetStatus(), request.actorId(), request.reason());

        return buildResponseFromEntity(
                incident,
                String.format("Transitioned from %s to %s. Reason: %s", previousStatus, request.targetStatus(), request.reason()),
                false
        );
    }

    private String computePayloadHash(IncidentRequest request) {
        try {
            return HashUtils.computeSha256(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request for SHA-256 idempotency hashing", e);
        }
    }

    private void saveIdempotencyRecord(String key, String hash, IncidentResponse response, Instant now) {
        try {
            String json = objectMapper.writeValueAsString(response);
            IdempotencyKeyRecord record = IdempotencyKeyRecord.builder()
                    .key(key)
                    .requestHash(hash)
                    .responseStatus(HttpStatus.CREATED.value())
                    .responseBody(json)
                    .createdAt(now)
                    .expiresAt(now.plus(Duration.ofHours(24))) // 24-hour retention TTL
                    .build();

            idempotencyKeyRepository.save(record);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for idempotency storage", e);
        }
    }

    private IncidentResponse deserializeCachedResponse(String responseBodyJson) {
        try {
            IncidentResponse cached = objectMapper.readValue(responseBodyJson, IncidentResponse.class);
            // Re-wrap with idempotentReplay flag = true
            return new IncidentResponse(
                    cached.incidentId(),
                    cached.source(),
                    cached.sourceEventId(),
                    cached.title(),
                    cached.serviceId(),
                    cached.status(),
                    cached.version(),
                    cached.isMajor(),
                    cached.classificationReason(),
                    cached.criteriaMet(),
                    cached.detectedAt(),
                    cached.awarenessAt(),
                    cached.classifiedAt(),
                    cached.classificationDeadline(),
                    cached.deadlineCountdownMinutes(),
                    cached.createdAt(),
                    true // idempotentReplay = true
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize cached response body", e);
        }
    }

    private IncidentResponse buildResponseFromEntity(Incident incident, String reason, boolean isReplay) {
        Long countdown = null;
        if (incident.getClassificationDeadline() != null) {
            countdown = Math.max(0, Duration.between(Instant.now(), incident.getClassificationDeadline()).toMinutes());
        }

        return new IncidentResponse(
                incident.getIncidentId(),
                incident.getSource(),
                incident.getSourceEventId(),
                incident.getTitle(),
                incident.getServiceId(),
                incident.getStatus().name(),
                incident.getVersion(),
                incident.getIsMajor(),
                reason,
                List.of(),
                incident.getDetectedAt(),
                incident.getAwarenessAt(),
                incident.getClassifiedAt(),
                incident.getClassificationDeadline(),
                countdown,
                incident.getCreatedAt(),
                isReplay
        );
    }
}
