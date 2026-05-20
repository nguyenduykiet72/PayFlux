CREATE TABLE t_payments
(
    id              UUID PRIMARY KEY,
    merchant_id     UUID         NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    amount_minor    BIGINT       NOT NULL CHECK (amount_minor > 0),
    currency        CHAR(3)      NOT NULL,
    provider        VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    version         INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (merchant_id, idempotency_key)
);

ALTER TABLE t_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_payments FORCE ROW LEVEL SECURITY;

CREATE
POLICY p_tenant ON t_payments
    USING (merchant_id::text = current_setting('app.current_merchant', true))
    WITH CHECK (merchant_id::text = current_setting('app.current_merchant', true));

CREATE INDEX idx_payments_merchant_status ON t_payments (merchant_id, status);
