CREATE TABLE delivery_failure
(
    id           UUID PRIMARY KEY,
    imap_folder  VARCHAR(255) NOT NULL,
    imap_uid     BIGINT       NOT NULL,
    recipient    VARCHAR(255),
    reason       VARCHAR(64)  NOT NULL,
    detail       VARCHAR(512),
    detected_at  TIMESTAMP    NOT NULL,
    UNIQUE (imap_folder, imap_uid)
);

CREATE INDEX idx_delivery_failure_detected_at ON delivery_failure (detected_at);
