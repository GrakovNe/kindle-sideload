package org.grakovne.sideload.kindle.stk.email.delivery.imap

import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.UIDFolder
import jakarta.mail.internet.MimeUtility
import jakarta.mail.search.FlagTerm
import mu.KotlinLogging
import org.grakovne.sideload.kindle.stk.email.delivery.configuration.DeliveryMonitorProperties
import org.springframework.stereotype.Component
import java.util.Properties

@Component
class ImapMailboxReader(
    private val properties: DeliveryMonitorProperties
) : MailboxReader {

    override fun fetchUnseen(handler: (RawEmailMessage) -> Boolean) {
        connect().use { store ->
            val folder = store.getFolder(properties.folder)
            folder.open(Folder.READ_WRITE)
            try {
                val unseen = folder.search(UNSEEN)
                logUnseen(folder, unseen)
                unseen.forEach { process(folder, it, handler) }
            } finally {
                folder.close(false)
            }
        }
    }

    private fun connect(): Store =
        Session.getInstance(Properties()).getStore(PROTOCOL).also {
            it.connect(properties.host, properties.port, properties.username, properties.password)
        }

    private fun logUnseen(folder: Folder, unseen: Array<Message>) = when (unseen.size) {
        0 -> logger.debug { "No unseen messages in ${folder.fullName}" }
        else -> logger.info { "Found ${unseen.size} unseen messages in ${folder.fullName}" }
    }

    private fun process(folder: Folder, message: Message, handler: (RawEmailMessage) -> Boolean) {
        val uid = (folder as UIDFolder).getUID(message)
        runCatching { toRawMessage(folder, message, uid) }
            .onSuccess { raw -> if (handler(raw)) message.setFlag(Flags.Flag.SEEN, true) }
            .onFailure { logger.warn(it) { "Failed to process message $uid in ${folder.fullName}, leaving it unseen" } }
    }

    private fun toRawMessage(folder: Folder, message: Message, uid: Long): RawEmailMessage =
        RawEmailMessage(
            folder = folder.fullName,
            uid = uid,
            subject = MimeUtility.decodeText(message.subject ?: ""),
            body = MimeTextExtractor.textOf(message),
        )

    companion object {
        private const val PROTOCOL = "imaps"
        private val UNSEEN = FlagTerm(Flags(Flags.Flag.SEEN), false)
        private val logger = KotlinLogging.logger {}
    }
}
