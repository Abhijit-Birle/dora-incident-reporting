CREATE SCHEMA IF NOT EXISTS incident;

CREATE TABLE incident.incident
(
    incident_id             UUID PRIMARY KEY,
    source                  VARCHAR(64)  NOT NULL,
    source_event_id         VARCHAR(128) NOT NULL,
    detected_at             TIMESTAMPTZ  NOT NULL,
    awareness_at            TIMESTAMPTZ  NOT NULL,
    classified_at           TIMESTAMPTZ,
    classification_deadline TIMESTAMPTZ,
    is_major                BOOLEAN,
    title                   VARCHAR(256) NOT NULL,
    description             TEXT,
    service_id              VARCHAR(128) NOT NULL,
    severity_hint           VARCHAR(16),
    raw_logs_ref            VARCHAR(512),
    status                  VARCHAR(32)  NOT NULL,
    version                 BIGINT       NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_incident_source_event ON incident.incident (source, source_event_id);
CREATE INDEX idx_incident_status ON incident.incident (status);
CREATE INDEX idx_incident_deadline ON incident.incident (classification_deadline) WHERE status = 'CLASSIFIED';

CREATE TABLE incident.idempotency_key
(
    key             VARCHAR(128) PRIMARY KEY,
    request_hash    VARCHAR(80) NOT NULL,
    response_status INT         NOT NULL,
    response_body   TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_idempotency_expires ON incident.idempotency_key (expires_at);
