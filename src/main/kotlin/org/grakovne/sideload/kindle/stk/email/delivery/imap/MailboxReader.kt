package org.grakovne.sideload.kindle.stk.email.delivery.imap

data class RawEmailMessage(
    val folder: String,
    val uid: Long,
    val subject: String,
    val body: String,
)

/** Where a previous scan stopped in a folder. */
data class ScanBaseline(
    val uidValidity: Long,
    val lastProcessedUid: Long,
)

/** How far the scan got, so the caller can persist it as the next baseline. */
data class ScanResult(
    val folder: String,
    val uidValidity: Long,
    val lastHandledUid: Long,
)

fun interface MailboxReader {

    /**
     * Hands every message in the monitored folder whose uid is above
     * [ScanBaseline.lastProcessedUid] to [handler]. The scan stops at the first message
     * the handler throws on, so the returned position never passes an unprocessed
     * message and the next scan retries it. A baseline recorded under a different
     * uidvalidity belongs to a recreated folder and is discarded: the scan starts over.
     *
     * The folder is opened read-only — the monitor never touches the `\Seen` flag, so
     * reading the mailbox does not mark anything as read for the user.
     */
    fun scan(baseline: ScanBaseline?, handler: (RawEmailMessage) -> Unit): ScanResult
}
