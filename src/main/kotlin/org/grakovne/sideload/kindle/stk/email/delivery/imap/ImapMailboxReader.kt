package org.grakovne.sideload.kindle.stk.email.delivery.imap

import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.UIDFolder
import jakarta.mail.internet.MimeUtility
import mu.KotlinLogging
import org.grakovne.sideload.kindle.stk.email.delivery.configuration.DeliveryMonitorProperties
import org.springframework.stereotype.Component
import java.util.Properties

/**
 * Reads the tail of the monitored mailbox over IMAP. The folder is opened READ_ONLY:
 * the monitor only ever advances its own baseline in the database, so nothing it
 * reads turns into "read" in the user's client.
 */
@Component
class ImapMailboxReader(
    private val properties: DeliveryMonitorProperties
) : MailboxReader {

    override fun scan(baseline: ScanBaseline?, handler: (RawEmailMessage) -> Unit): ScanResult {
        connect().use { store ->
            val folder = store.getFolder(properties.folder)
            folder.open(Folder.READ_ONLY)
            try {
                return scanFolder(folder, baseline, handler)
            } finally {
                folder.close(false)
            }
        }
    }

    private fun scanFolder(
        folder: Folder,
        baseline: ScanBaseline?,
        handler: (RawEmailMessage) -> Unit,
    ): ScanResult {
        val uidFolder = folder as UIDFolder
        val uidValidity = uidFolder.uidValidity
        val lastProcessed = startUid(baseline, uidValidity, uidFolder.uidNext, properties.backfillHistory)
        val messages = uidFolder.getMessagesByUID(lastProcessed + 1, UIDFolder.MAXUID)
        logFound(folder.fullName, messages.size, lastProcessed)

        // lazy, so a handler failure stops the scan without decoding the rest of the batch
        val read = messages.asSequence().map { message ->
            val uid = uidFolder.getUID(message)
            uid to readMessage(message, uid)
        }

        return ScanResult(
            folder = folder.fullName,
            uidValidity = uidValidity,
            lastHandledUid = drain(read, lastProcessed, handler),
        )
    }

    private fun connect(): Store =
        Session.getInstance(Properties()).getStore(PROTOCOL).also {
            it.connect(properties.host, properties.port, properties.username, properties.password)
        }

    private fun logFound(folder: String, found: Int, lastProcessed: Long) = when (found) {
        0 -> logger.debug { "No messages above uid $lastProcessed in $folder" }
        else -> logger.info { "Found $found messages above uid $lastProcessed in $folder" }
    }

    /** Null when the message cannot be decoded at all — see [drain]. */
    private fun readMessage(message: Message, uid: Long): RawEmailMessage? =
        runCatching {
            RawEmailMessage(
                folder = message.folder.fullName,
                uid = uid,
                subject = MimeUtility.decodeText(message.subject ?: ""),
                body = MimeTextExtractor.textOf(message),
            )
        }.onFailure { logger.warn(it) { "Skipping undecodable message $uid in ${message.folder.fullName}" } }
            .getOrNull()

    companion object {
        private const val PROTOCOL = "imaps"
        private val logger = KotlinLogging.logger {}

        /**
         * The highest uid already handled — the scan starts just above it. A baseline
         * from another uidvalidity belongs to a recreated folder and is discarded.
         * Without a baseline the first scan starts just past the newest message, so
         * enabling the monitor does not re-read the whole mailbox and count years of
         * old bounces as fresh ones; `backfill-history: true` opts into that.
         */
        internal fun startUid(
            baseline: ScanBaseline?,
            uidValidity: Long,
            uidNext: Long,
            backfillHistory: Boolean,
        ): Long = baseline
            ?.takeIf { it.uidValidity == uidValidity }
            ?.lastProcessedUid
            ?: if (backfillHistory) 0L else uidNext - 1

        /**
         * Walks the new messages oldest first and returns the highest uid dealt with.
         *
         * The two ways a message can fail are deliberately handled differently:
         *  - a null entry means the message could not be decoded. That is permanent, so
         *    it is skipped *and counted as handled*: refusing to move past it would wedge
         *    the monitor on that uid forever and hide every later bounce. The cost is that
         *    one unreadable message is never classified.
         *  - [handler] throwing (the database being down, say) is worth retrying, so it
         *    propagates and no position is returned — the caller keeps the old baseline
         *    and the next poll retries from there.
         */
        internal fun drain(
            read: Sequence<Pair<Long, RawEmailMessage?>>,
            lastProcessed: Long,
            handler: (RawEmailMessage) -> Unit,
        ): Long = read.fold(lastProcessed) { handled, (uid, message) ->
            if (uid <= lastProcessed) return@fold handled
            message?.let(handler)
            maxOf(handled, uid)
        }
    }
}
