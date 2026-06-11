CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    customer_email TEXT NOT NULL,
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type TEXT NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type TEXT NOT NULL,
    routing_key TEXT NOT NULL,
    payload TEXT NOT NULL,
    published_at TIMESTAMPTZ NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_unpublished
ON outbox_events (created_at)
WHERE published_at IS NULL;

CREATE TABLE IF NOT EXISTS processed_messages (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS notification_log (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    event_id UUID NOT NULL,
    recipient_email TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
