CREATE TABLE IF NOT EXISTS convertation_task (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    source_file_url VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    fail_reason VARCHAR(255),
    status VARCHAR(255) NOT NULL
);