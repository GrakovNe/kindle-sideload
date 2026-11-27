package org.grakovne.sideload.kindle.stk.email.delivery.imap

import jakarta.mail.MessagingException
import jakarta.mail.Multipart
import jakarta.mail.Part
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Flattens a MIME tree into the text of its `text/plain` parts. Attachments,
 * html bodies and embedded `message/rfc822` copies of the original mail are
 * ignored: bounce signatures live in the plain-text parts.
 */
object MimeTextExtractor {

    fun textOf(part: Part): String = collect(part).joinToString("\n")

    private fun collect(part: Part): List<String> = try {
        when {
            part.isMimeType(PLAIN) -> listOf(decode(part.content))
            part.isMimeType(MULTIPART) -> partsOf(part).flatMap { collect(it) }
            else -> emptyList()
        }
    } catch (e: MessagingException) {
        logger.warn(e) { "Skipped unreadable MIME part" }
        emptyList()
    } catch (e: IOException) {
        logger.warn(e) { "Skipped unreadable MIME part" }
        emptyList()
    }

    private fun partsOf(part: Part): List<Part> =
        (part.content as Multipart).let { multipart ->
            (0 until multipart.count).map { multipart.getBodyPart(it) }
        }

    private fun decode(content: Any?): String = when (content) {
        is String -> content

        is InputStream -> ByteArrayOutputStream().use { buffer ->
            content.copyTo(buffer)
            buffer.toString(Charsets.UTF_8.name())
        }

        else -> content?.toString() ?: ""
    }

    private const val PLAIN = "text/plain"
    private const val MULTIPART = "multipart/*"
    private val logger = KotlinLogging.logger {}
}
