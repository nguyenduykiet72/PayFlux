CREATE TABLE outbox_events
(
    id            UUID PRIMARY KEY,
    aggregate_id  UUID         NOT NULL,
    event_type    VARCHAR(64)  NOT NULL,
    payload       JSONB        NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_created_at ON outbox_events (created_at);

GRANT SELECT, INSERT ON outbox_events TO payflux_app;

CREATE PUBLICATION dbz_pub FOR TABLE outbox_events;
