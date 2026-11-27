-- Local baseline of the mailbox scan: the highest IMAP UID already handled per folder.
-- uid_validity guards the mark: IMAP uids are only meaningful within one uidvalidity
-- epoch, so a recreated folder invalidates the stored position instead of silently
-- skipping or replaying messages.
CREATE TABLE email_baseline
(
    imap_folder        VARCHAR(255) PRIMARY KEY,
    uid_validity       BIGINT       NOT NULL,
    last_processed_uid BIGINT       NOT NULL,
    updated_at         TIMESTAMP    NOT NULL
);
