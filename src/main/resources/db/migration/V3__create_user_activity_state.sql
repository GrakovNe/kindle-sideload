CREATE TABLE user_activity_state (
    id UUID PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    activity_state VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL
);