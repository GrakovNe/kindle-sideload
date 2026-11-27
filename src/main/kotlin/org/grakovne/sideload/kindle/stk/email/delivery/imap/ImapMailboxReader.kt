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

        var lastHandled = lastProcessed
        for (message in messages) {
            val uid = uidFolder.getUID(message)
            if (uid <= lastProcessed) continue
            try {
                handler(toRawMessage(message, uid))
            } catch (e: Exception) {
                logger.warn(e) { "Failed to process message $uid, stopping the scan" }
                break
            }
            lastHandled = maxOf(lastHandled, uid)
        }

        return ScanResult(
            folder = folder.fullName,
            uidValidity = uidValidity,
            lastHandledUid = lastHandled,
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

    private fun toRawMessage(message: Message, uid: Long): RawEmailMessage =
        RawEmailMessage(
            folder = message.folder.fullName,
            uid = uid,
            subject = MimeUtility.decodeText(message.subject ?: ""),
            body = MimeTextExtractor.textOf(message),
        )

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
    }
}
