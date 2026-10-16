CREATE TABLE IF NOT EXISTS converter.inbox (
    event_id UUID PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    created_at_time TIMESTAMPTZ NOT NULL,
    processed_at_time TIMESTAMPTZ
);