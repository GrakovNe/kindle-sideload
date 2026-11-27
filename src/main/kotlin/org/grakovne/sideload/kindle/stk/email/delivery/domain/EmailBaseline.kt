package org.grakovne.sideload.kindle.stk.email.delivery.domain

import java.time.LocalDateTime

/**
 * How far the monitor has already read a mailbox, kept locally instead of in the
 * IMAP `\Seen` flag.
 *
 * [uidValidity] is the epoch the [lastProcessedUid] belongs to: IMAP uids are only
 * comparable within one uidvalidity, so a recreated folder invalidates the mark
 * rather than silently skipping or replaying messages.
 */
data class EmailBaseline(
    val imapFolder: String,
    val uidValidity: Long,
    val lastProcessedUid: Long,
    val updatedAt: LocalDateTime,
)
