CREATE TABLE IF NOT EXISTS convertation_task (
    id UUID PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    source_file_url VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL,
    fail_reason VARCHAR,
    status VARCHAR NOT NULL
);