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

/**
 * Implementation of {@link IncidentService} coordinating idempotency verification, RTS criteria evaluation,
 * statutory deadline computation, JPA persistence, and state machine transitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final RtsClassificationEngine classificationEngine;
    private final ObjectMapper objectMapper;

    /**
     * Executes the end-to-end incident ingestion pipeline under transactional boundaries.
     * <p>
     * Steps:
     * <ol>
     *   <li>Computes SHA-256 hash of the incoming request JSON payload.</li>
     *   <li>Verifies {@code idempotencyKey} against the database cache. If present with an identical hash,
     *       returns the cached response marked with {@code idempotentReplay = true}. If the hash differs,
     *       throws an {@link IdempotencyConflictException}.</li>
     *   <li>Checks for duplicate source events using {@code (source, sourceEventId)} to guard against unkeyed retries.</li>
     *   <li>Evaluates the incident against RTS (EU 2024/1772) specification criteria via {@link RtsClassificationEngine}.</li>
     *   <li>If classified as MAJOR, computes the statutory dual-deadline:
     *       {@code deadline = min(classifiedAt + 4 hours, awarenessAt + 24 hours)}.</li>
     *   <li>Persists the new {@link Incident} entity in {@link IncidentStatus#CLASSIFIED} state with version 0.</li>
     *   <li>Caches the response payload in the {@code incident.idempotency_key} table with a 24-hour expiration TTL.</li>
     * </ol>
     * </p>
     *
     * @param idempotencyKey unique client idempotency token or generated fallback
     * @param request        incoming incident attributes and regulatory metrics
     * @return {@link IncidentResponse} containing classification decision and deadline metrics
     * @throws IdempotencyConflictException if the idempotency key was previously bound to a different request payload
     */
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

    /**
     * Retrieves an incident by its UUID in a read-only transaction, dynamically calculating the remaining countdown.
     *
     * @param incidentId the unique identifier of the incident to fetch
     * @return {@link IncidentResponse} containing current state and updated deadline countdown minutes
     * @throws IncidentNotFoundException if the specified incident ID does not exist in the database
     */
    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getIncident(UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        return buildResponseFromEntity(incident, "Incident status: " + incident.getStatus(), false);
    }

    /**
     * Lists incidents with optional status and severity filtering, returning a paginated result set.
     *
     * @param status   optional {@link IncidentStatus} filter (null for all statuses)
     * @param isMajor  optional major incident flag filter (null for all severities)
     * @param pageable pagination parameters (page number, size, sort)
     * @return a {@link Page} of mapped {@link IncidentResponse} objects
     */
    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> listIncidents(IncidentStatus status, Boolean isMajor, Pageable pageable) {
        Page<Incident> page = incidentRepository.findByFilters(status, isMajor, pageable);
        return page.map(incident -> buildResponseFromEntity(incident, "Incident status: "
                + incident.getStatus(), false));
    }

    /**
     * Transitions an incident to a new lifecycle state enforcing optimistic locking and FSM rules.
     * <p>
     * Throws an {@link OptimisticLockingFailureException} if the client's provided version does not match
     * the database version, protecting against concurrent supervisor approvals or stale state overrides.
     * </p>
     *
     * @param incidentId the unique identifier of the incident to advance
     * @param request    transition details including target status, version token, actor, and rationale
     * @return {@link IncidentResponse} reflecting the newly persisted state and incremented version
     * @throws IncidentNotFoundException           if the incident does not exist
     * @throws OptimisticLockingFailureException   if the version token does not match the stored version
     * @throws IllegalStateException               if the state transition violates {@link IncidentStatus#canTransitionTo}
     */
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

    /**
     * Computes the SHA-256 fingerprint of the serialized {@link IncidentRequest} JSON string.
     *
     * @param request the incoming incident intake request
     * @return 64-character lowercase hex-encoded SHA-256 hash string
     * @throws RuntimeException if JSON serialization fails
     */
    private String computePayloadHash(IncidentRequest request) {
        try {
            return HashUtils.computeSha256(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request for SHA-256 idempotency hashing", e);
        }
    }

    /**
     * Persists an idempotency record caching the response body with a 24-hour expiration window.
     *
     * @param key      the idempotency key
     * @param hash     the SHA-256 fingerprint of the request payload
     * @param response the generated incident response to cache
     * @param now      the current instant marking record creation
     */
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

    /**
     * Deserializes a cached JSON response body from the idempotency table, setting {@code idempotentReplay = true}.
     *
     * @param responseBodyJson serialized JSON string from {@link IdempotencyKeyRecord#getResponseBody()}
     * @return reconstructed {@link IncidentResponse} marked as an idempotent replay
     * @throws RuntimeException if deserialization fails
     */
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

    /**
     * Reconstructs an {@link IncidentResponse} DTO from an {@link Incident} entity, recalculating the live countdown.
     *
     * @param incident the source incident entity
     * @param reason   classification rationale or status change description
     * @param isReplay flag indicating whether this is an idempotent replay response
     * @return complete {@link IncidentResponse} DTO with active statutory countdown
     */
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
