package io.doraauditor.incident_svc.service;

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
import io.doraauditor.incident_svc.service.impl.IncidentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceImplTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private RtsClassificationEngine classificationEngine;

    private ObjectMapper objectMapper;
    private IncidentServiceImpl incidentService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        incidentService = new IncidentServiceImpl(
                incidentRepository,
                idempotencyKeyRepository,
                classificationEngine,
                objectMapper
        );
    }

    private Incident createMockIncident(UUID id, IncidentStatus status, Long version) {
        Instant now = Instant.now();
        return Incident.builder()
                .incidentId(id)
                .source("DATADOG")
                .sourceEventId("alert-001")
                .detectedAt(now)
                .awarenessAt(now)
                .classifiedAt(now)
                .classificationDeadline(now.plusSeconds(3600))
                .isMajor(true)
                .title("Test Major Incident")
                .serviceId("payment-gateway")
                .status(status)
                .version(version)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("getIncident should return IncidentResponse when found")
    void getIncident_Success() {
        UUID id = UUID.randomUUID();
        Incident incident = createMockIncident(id, IncidentStatus.CLASSIFIED, 0L);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));

        IncidentResponse response = incidentService.getIncident(id);

        assertThat(response).isNotNull();
        assertThat(response.incidentId()).isEqualTo(id);
        assertThat(response.status()).isEqualTo("CLASSIFIED");
        assertThat(response.isMajor()).isTrue();
    }

    @Test
    @DisplayName("getIncident should throw IncidentNotFoundException when ID does not exist")
    void getIncident_NotFound() {
        UUID id = UUID.randomUUID();
        when(incidentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getIncident(id))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    @DisplayName("transitionStatus should succeed and advance FSM when version matches")
    void transitionStatus_Success() {
        UUID id = UUID.randomUUID();
        Incident incident = createMockIncident(id, IncidentStatus.CLASSIFIED, 0L);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransitionStatusRequest request = new TransitionStatusRequest(
                IncidentStatus.DRAFTING,
                0L,
                "AI copilot drafting requested",
                "system-orchestrator"
        );

        IncidentResponse response = incidentService.transitionStatus(id, request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("DRAFTING");
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.DRAFTING);
    }

    @Test
    @DisplayName("transitionStatus should throw OptimisticLockingFailureException when version does not match")
    void transitionStatus_VersionMismatch() {
        UUID id = UUID.randomUUID();
        Incident incident = createMockIncident(id, IncidentStatus.CLASSIFIED, 1L); // current version is 1
        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));

        TransitionStatusRequest request = new TransitionStatusRequest(
                IncidentStatus.DRAFTING,
                0L, // stale client version 0
                "AI copilot drafting requested",
                "analyst-1"
        );

        assertThatThrownBy(() -> incidentService.transitionStatus(id, request))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessageContaining("Incident version conflict");

        verify(incidentRepository, never()).save(any());
    }

    @Test
    @DisplayName("transitionStatus should throw IllegalStateException when transition violates FSM matrix")
    void transitionStatus_IllegalTransition() {
        UUID id = UUID.randomUUID();
        Incident incident = createMockIncident(id, IncidentStatus.CLASSIFIED, 0L);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));

        // CLASSIFIED directly to SUBMITTED is strictly forbidden by FSM
        TransitionStatusRequest request = new TransitionStatusRequest(
                IncidentStatus.SUBMITTED,
                0L,
                "Force submit",
                "analyst-1"
        );

        assertThatThrownBy(() -> incidentService.transitionStatus(id, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Illegal state transition");

        verify(incidentRepository, never()).save(any());
    }

    @Test
    @DisplayName("listIncidents should return paginated incidents matching filters")
    void listIncidents_Success() {
        Incident incident = createMockIncident(UUID.randomUUID(), IncidentStatus.CLASSIFIED, 0L);
        Page<Incident> page = new PageImpl<>(List.of(incident));
        PageRequest pageable = PageRequest.of(0, 10);

        when(incidentRepository.findByFilters(IncidentStatus.CLASSIFIED, true, pageable)).thenReturn(page);

        Page<IncidentResponse> result = incidentService.listIncidents(IncidentStatus.CLASSIFIED, true, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().status()).isEqualTo("CLASSIFIED");
    }

    @Test
    @DisplayName("processIncident should return cached response on identical idempotency replay")
    void processIncident_IdempotentReplay() throws Exception {
        String key = "key-123";
        Instant now = Instant.now();
        IncidentRequest request = new IncidentRequest(
                "DATADOG",
                "evt-99",
                now,
                now,
                "API Outage",
                "details",
                "payment-api",
                "HIGH",
                "s3://logs",
                true,
                150_000L,
                15.0,
                180L,
                BigDecimal.valueOf(200_000),
                false,
                false,
                1
        );

        String jsonRequest = objectMapper.writeValueAsString(request);
        String expectedHash = io.doraauditor.incident_svc.util.HashUtils.computeSha256(jsonRequest);

        IncidentResponse cachedResponse = new IncidentResponse(
                UUID.randomUUID(),
                "DATADOG",
                "evt-99",
                "API Outage",
                "payment-api",
                "CLASSIFIED",
                0L,
                true,
                "major incident",
                List.of(),
                now,
                now,
                now,
                now.plusSeconds(3600),
                60L,
                now,
                false
        );

        IdempotencyKeyRecord record = new IdempotencyKeyRecord(
                key,
                expectedHash,
                201,
                objectMapper.writeValueAsString(cachedResponse),
                now,
                now.plusSeconds(86400)
        );

        when(idempotencyKeyRepository.findById(key)).thenReturn(Optional.of(record));

        IncidentResponse response = incidentService.processIncident(key, request);

        assertThat(response).isNotNull();
        assertThat(response.idempotentReplay()).isTrue();
        verify(incidentRepository, never()).save(any());
    }

    @Test
    @DisplayName("processIncident should throw IdempotencyConflictException when key reused with different body")
    void processIncident_PayloadMismatch() {
        String key = "key-123";
        Instant now = Instant.now();
        IncidentRequest request = new IncidentRequest(
                "DATADOG",
                "evt-99",
                now,
                now,
                "API Outage",
                "details",
                "payment-api",
                "HIGH",
                "s3://logs",
                true,
                150_000L,
                15.0,
                180L,
                BigDecimal.valueOf(200_000),
                false,
                false,
                1
        );

        IdempotencyKeyRecord record = new IdempotencyKeyRecord(
                key,
                "DIFFERENT_HASH_1234567890abcdef",
                201,
                "{}",
                now,
                now.plusSeconds(86400)
        );

        when(idempotencyKeyRepository.findById(key)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> incidentService.processIncident(key, request))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("previously used with a different request payload");

        verify(incidentRepository, never()).save(any());
    }
}
