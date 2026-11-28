-- The sender address we have already announced to users. One row: the app sends from a single
-- address (spring.mail.from), so the state is a singleton, like converter_binary_reference.
-- generation bumps on every re-announcement (address change or a forced admin re-announce) and
-- feeds the notification dedup key, so a forced re-announce is not blocked by the unique index.
CREATE TABLE sender_address_announcement
(
    id           VARCHAR(64) PRIMARY KEY,
    address      VARCHAR(255),
    generation   INT       NOT NULL DEFAULT 0,
    announced_at TIMESTAMP
);

-- Outbound Telegram notifications with a schedule baked in at enqueue time. next_attempt_at is
-- absolute rather than a rate limiter in memory: a restart mid-campaign resumes on schedule
-- instead of racing to catch up, and the worker can only ever send what is already due.
CREATE TABLE user_notification
(
    id              UUID PRIMARY KEY,
    dedup_key       VARCHAR(255) NOT NULL UNIQUE,
    user_id         VARCHAR      NOT NULL,
    kind            VARCHAR(64)  NOT NULL,
    generation      INT          NOT NULL DEFAULT 0,
    payload         VARCHAR(512),
    status          VARCHAR(32)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL,
    next_attempt_at TIMESTAMP,
    sent_at         TIMESTAMP
);

CREATE INDEX idx_user_notification_due ON user_notification (status, next_attempt_at);
