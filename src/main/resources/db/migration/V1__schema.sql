CREATE TABLE beds (
    id          VARCHAR(64) PRIMARY KEY,
    ward_id     VARCHAR(64) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    occupant_id VARCHAR(64),
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT chk_occupied_has_patient CHECK (status <> 'OCCUPIED' OR occupant_id IS NOT NULL)
);
CREATE INDEX idx_beds_ward_status ON beds (ward_id, status);

-- Outbox transaccional: los eventos se guardan junto al cambio de estado
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    aggregate_id   VARCHAR(64)  NOT NULL,
    topic          VARCHAR(128) NOT NULL,
    event_type     VARCHAR(64)  NOT NULL,
    schema_version INT          NOT NULL,
    payload        TEXT         NOT NULL,
    correlation_id VARCHAR(64),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at   TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_outbox_pending ON outbox_events (published_at, created_at);

-- Inbox para consumidores idempotentes
CREATE TABLE processed_messages (
    message_id   VARCHAR(128) PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
