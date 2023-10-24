CREATE TABLE IF NOT EXISTS transfer_email_task (
    id UUID PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    environment_id VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL,
    fail_reason VARCHAR,
    status VARCHAR NOT NULL
);