package io.doraauditor.incident_svc.rules;

import java.util.List;

/**
 * Output of the deterministic RTS (EU 2024/1772) evaluation.
 */
public record ClassificationResult(
        boolean isMajor,
        String reason,
        List<String> criteriaMet
) {}
