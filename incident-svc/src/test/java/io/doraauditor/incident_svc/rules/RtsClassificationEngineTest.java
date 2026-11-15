package io.doraauditor.incident_svc.rules;

import io.doraauditor.incident_svc.dto.IncidentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RtsClassificationEngineTest {

    private RtsClassificationEngine engine;

    @BeforeEach
    void setUp() {
        List<RegulatoryCriterion> criteria = List.of(
                new CriticalServiceCriterion(),
                new AffectedClientsCriterion(),
                new EconomicImpactCriterion(),
                new DurationCriterion(),
                new DataIntegrityCriterion(),
                new ReputationalImpactCriterion(),
                new GeographicSpreadCriterion()
        );
        engine = new RtsClassificationEngine(criteria);
    }

    private IncidentRequest createRequest(
            boolean isCritical,
            Long clients,
            Double clientPct,
            Long duration,
            BigDecimal economicImpact,
            boolean dataLoss,
            boolean reputationalImpact,
            Integer geoSpread
    ) {
        Instant now = Instant.now();
        return new IncidentRequest(
                "DATADOG",
                "evt-101",
                now,
                now,
                "Payment Latency Spike",
                "Detailed anomaly log",
                "payment-api",
                "CRITICAL",
                "s3://logs/payment-101.log",
                isCritical,
                clients,
                clientPct,
                duration,
                economicImpact,
                dataLoss,
                reputationalImpact,
                geoSpread
        );
    }

    @Test
    @DisplayName("Should classify as NON-MAJOR when service is not critical even with high client impact")
    void shouldClassifyNonMajor_WhenServiceNotCritical() {
        IncidentRequest request = createRequest(
                false, // not critical
                500_000L,
                25.0,
                300L,
                new BigDecimal("500000.00"),
                false,
                true,
                3
        );

        ClassificationResult result = engine.evaluate(request);

        assertThat(result.isMajor()).isFalse();
        assertThat(result.reason()).contains("not designated as a critical");
    }

    @Test
    @DisplayName("Should classify as MAJOR when critical service and data integrity compromised")
    void shouldClassifyMajor_WhenDataIntegrityCompromised() {
        IncidentRequest request = createRequest(
                true, // critical
                100L,
                0.1,
                15L,
                BigDecimal.ZERO,
                true, // data loss
                false,
                1
        );

        ClassificationResult result = engine.evaluate(request);

        assertThat(result.isMajor()).isTrue();
        assertThat(result.criteriaMet()).contains("CRITICAL_SERVICE_AFFECTED", "DATA_INTEGRITY_COMPROMISED");
        assertThat(result.reason()).contains("data loss/integrity compromise");
    }

    @Test
    @DisplayName("Should classify as MAJOR when critical service meets 2 materiality thresholds (clients + duration)")
    void shouldClassifyMajor_WhenTwoMaterialityThresholdsMet() {
        IncidentRequest request = createRequest(
                true, // critical
                150_000L, // Threshold > 100k
                5.0,
                150L, // Threshold > 120m
                BigDecimal.ZERO,
                false,
                false,
                1
        );

        ClassificationResult result = engine.evaluate(request);

        assertThat(result.isMajor()).isTrue();
        assertThat(result.criteriaMet()).contains(
                "CRITICAL_SERVICE_AFFECTED",
                "CLIENTS_AFFECTED_THRESHOLD_EXCEEDED",
                "DOWNTIME_EXCEEDS_2_HOURS"
        );
        assertThat(result.reason()).contains("2 materiality thresholds met");
    }

    @Test
    @DisplayName("Should classify as NON-MAJOR when critical service meets only 1 materiality threshold")
    void shouldClassifyNonMajor_WhenOnlyOneMaterialityThresholdMet() {
        IncidentRequest request = createRequest(
                true, // critical
                150_000L, // Only 1 threshold (>100k)
                2.0,
                30L, // < 120m
                new BigDecimal("5000.00"), // < €100k
                false,
                false,
                1
        );

        ClassificationResult result = engine.evaluate(request);

        assertThat(result.isMajor()).isFalse();
        assertThat(result.reason()).contains("only 1 of required 2 materiality thresholds met");
    }

    @Test
    @DisplayName("Should classify as MAJOR when economic impact > 100k EUR and geographic spread >= 2 Member States")
    void shouldClassifyMajor_WhenEconomicAndGeographicThresholdsMet() {
        IncidentRequest request = createRequest(
                true, // critical
                10_000L,
                1.0,
                45L,
                new BigDecimal("250000.00"), // > €100k
                false,
                false,
                3 // >= 2 Member States
        );

        ClassificationResult result = engine.evaluate(request);

        assertThat(result.isMajor()).isTrue();
        assertThat(result.criteriaMet()).contains(
                "CRITICAL_SERVICE_AFFECTED",
                "ECONOMIC_IMPACT_EXCEEDS_100K",
                "CROSS_BORDER_GEOGRAPHIC_SPREAD"
        );
    }
}
