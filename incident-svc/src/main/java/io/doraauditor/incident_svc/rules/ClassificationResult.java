package io.doraauditor.incident_svc.rules;

import java.util.List;

/**
 * Output model encapsulating the outcome of a deterministic RTS (EU 2024/1772) Article 9 evaluation.
 *
 * @param isMajor     whether the incident meets the statutory threshold to be classified as a MAJOR ICT incident
 * @param reason      human-readable regulatory rationale citing the triggered criteria and legal basis
 * @param criteriaMet list of machine-readable criterion codes (e.g. {@code "CRITICAL_SERVICE_AFFECTED"}) that were breached
 */
public record ClassificationResult(
        boolean isMajor,
        String reason,
        List<String> criteriaMet
) {}
