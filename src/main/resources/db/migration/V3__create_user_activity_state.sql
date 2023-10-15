CREATE TABLE user_activity_state (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    activity_state VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);