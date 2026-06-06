CREATE TABLE payments (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID           NOT NULL REFERENCES orders(id),
    idempotency_key  VARCHAR(255)   NOT NULL UNIQUE,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    amount           NUMERIC(10, 2) NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT payments_status_check CHECK (status IN ('PENDING', 'CAPTURED', 'REFUNDED'))
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
