package org.grakovne.sideload.kindle.delivery.imap

import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.UIDFolder
import jakarta.mail.internet.MimeUtility
import jakarta.mail.search.FlagTerm
import mu.KotlinLogging
import org.grakovne.sideload.kindle.delivery.configuration.DeliveryMonitorProperties
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Properties

@Component
class ImapMailboxReader(
    private val properties: DeliveryMonitorProperties
) : MailboxReader {

    override fun fetchUnseen(handler: (RawEmailMessage) -> Boolean) {
        val store = connect()
        try {
            val folder = store.getFolder(properties.folder)
            folder.open(Folder.READ_WRITE)
            try {
                val unseen = folder.search(UNSEEN)
                if (unseen.isEmpty()) {
                    logger.debug("No unseen messages in ${folder.fullName}")
                } else {
                    logger.info("Found ${unseen.size} unseen messages in ${folder.fullName}")
                }
                unseen.forEach { process(folder, it, handler) }
            } finally {
                folder.close(false)
            }
        } finally {
            store.close()
        }
    }

    private fun connect(): Store {
        val session = Session.getInstance(Properties())
        val store = session.getStore(PROTOCOL)
        store.connect(properties.host, properties.port, properties.username, properties.password)
        return store
    }

    private fun process(
        folder: Folder,
        message: Message,
        handler: (RawEmailMessage) -> Boolean
    ) {
        val uid = (folder as UIDFolder).getUID(message)
        try {
            val raw = RawEmailMessage(
                folder = folder.fullName,
                uid = uid,
                subject = MimeUtility.decodeText(message.subject ?: ""),
                body = extractText(message),
            )

            if (handler(raw)) {
                message.setFlag(Flags.Flag.SEEN, true)
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to process message $uid in ${folder.fullName}, leaving it unseen" }
        }
    }

    private fun extractText(message: Message): String {
        val builder = StringBuilder()
        appendText(message, builder)
        return builder.toString()
    }

    private fun appendText(part: Part, builder: StringBuilder) {
        try {
            if (part.isMimeType("text/plain")) {
                builder.append(decode(part.content)).append('\n')
                return
            }

            val content = part.content
            if (content is Multipart) {
                (0 until content.count).forEach { appendText(content.getBodyPart(it), builder) }
            }
        } catch (e: MessagingException) {
            logger.warn(e) { "Skipped unreadable MIME part" }
        } catch (e: IOException) {
            logger.warn(e) { "Skipped unreadable MIME part" }
        }
    }

    private fun decode(content: Any?): String = when (content) {
        is String -> content
        is InputStream -> readFully(content)
        else -> content?.toString() ?: ""
    }

    private fun readFully(stream: InputStream): String {
        val buffer = ByteArrayOutputStream()
        stream.use { it.copyTo(buffer) }
        return buffer.toString(Charsets.UTF_8.name())
    }

    companion object {
        private const val PROTOCOL = "imaps"
        private val UNSEEN = FlagTerm(Flags(Flags.Flag.SEEN), false)
        private val logger = KotlinLogging.logger {}
    }
}
