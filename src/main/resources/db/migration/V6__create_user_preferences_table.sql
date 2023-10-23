CREATE TABLE user_preferences (
    id UUID PRIMARY KEY,
    user_id TEXT NOT NULL,
    output_format TEXT,
    email TEXT
);