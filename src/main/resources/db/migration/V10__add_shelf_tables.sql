CREATE TABLE shelf_reference (
    id UUID PRIMARY KEY,
    short_id VARCHAR NOT NULL UNIQUE,
    user_id VARCHAR NOT NULL UNIQUE
);

CREATE TABLE shelf_item (
    id VARCHAR PRIMARY KEY,
    shelf_id UUID NOT NULL,
    environment_id VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL,
    status VARCHAR NOT NULL
);


