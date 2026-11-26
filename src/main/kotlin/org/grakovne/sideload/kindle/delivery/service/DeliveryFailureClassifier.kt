package org.grakovne.sideload.kindle.delivery.service

import org.grakovne.sideload.kindle.delivery.domain.DeliveryFailure
import org.grakovne.sideload.kindle.delivery.domain.DeliveryFailureReason
import org.grakovne.sideload.kindle.delivery.imap.RawEmailMessage
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class DeliveryFailureClassifier {

    fun classify(message: RawEmailMessage): DeliveryFailure? {
        val haystack = "${message.subject}\n${message.body}"

        amazonProblemNotice(haystack, message)?.let { return it }

        val status = DSN_STATUS.find(haystack)?.groupValues?.get(1)
        if (status != null) {
            return if (status.startsWith("2")) null else dsnFailure(status, haystack, message)
        }

        if (BOUNCE_SUBJECTS.any { it.containsMatchIn(message.subject) }) {
            return failure(message, DeliveryFailureReason.BOUNCED, message.subject.trim())
        }

        return null
    }

    private fun amazonProblemNotice(
        haystack: String,
        message: RawEmailMessage
    ): DeliveryFailure? {
        val code = AMAZON_ERROR_CODE.find(haystack)?.groupValues?.get(1)
            ?: AMAZON_BODY_ERROR.find(haystack)?.groupValues?.get(1)
            ?: return null

        val reason = AMAZON_REASON_CODES[code] ?: DeliveryFailureReason.UNKNOWN
        val detail = AMAZON_ERROR_LINE.find(haystack)?.value?.trim()

        return failure(message, reason, detail)
    }

    private fun dsnFailure(
        status: String,
        haystack: String,
        message: RawEmailMessage
    ): DeliveryFailure {
        val recipient = DSN_FINAL_RECIPIENT.find(haystack)?.groupValues?.get(1)
        val diagnostic = DSN_DIAGNOSTIC.find(haystack)?.groupValues?.get(1)?.trim()
        val detail = listOfNotNull(statusLine(haystack), diagnostic?.let { "Diagnostic-Code: $it" })
            .joinToString(" | ")
            .ifBlank { null }

        val reason = when {
            MALICIOUS.containsMatchIn(haystack) -> DeliveryFailureReason.REJECTED_AS_MALICIOUS

            status in UNKNOWN_RECIPIENT_STATUSES ||
                UNKNOWN_RECIPIENT.containsMatchIn(haystack) -> DeliveryFailureReason.UNKNOWN_RECIPIENT

            else -> DeliveryFailureReason.BOUNCED
        }

        return failure(message, reason, detail, recipient)
    }

    private fun statusLine(haystack: String): String? =
        haystack.lineSequence()
            .firstOrNull { it.contains("Status:") }
            ?.trim()

    private fun failure(
        message: RawEmailMessage,
        reason: DeliveryFailureReason,
        detail: String?,
        recipient: String? = null,
    ): DeliveryFailure =
        DeliveryFailure(
            id = UUID.randomUUID(),
            imapFolder = message.folder,
            imapUid = message.uid,
            recipient = recipient ?: KINDLE_ADDRESS.find(message.body)?.value,
            reason = reason,
            detail = detail?.take(MAX_DETAIL_LENGTH),
            detectedAt = LocalDateTime.now(),
        )

    companion object {
        private const val MAX_DETAIL_LENGTH = 500

        private val AMAZON_ERROR_CODE = Regex(
            "problem with the document\\(s\\) you sent to Kindle[\\s\\S]*?due to (E\\d{3})",
            RegexOption.IGNORE_CASE,
        )
        private val AMAZON_BODY_ERROR =
            Regex("could not be delivered due to (E\\d{3})", RegexOption.IGNORE_CASE)
        private val AMAZON_ERROR_LINE = Regex("could not be delivered due to E\\d{3}[^\\n]*")
        private val AMAZON_REASON_CODES = mapOf(
            "E001" to DeliveryFailureReason.UNSUPPORTED_FORMAT,
            "E009" to DeliveryFailureReason.NO_ATTACHMENT,
            "E010" to DeliveryFailureReason.THROTTLED,
            "E011" to DeliveryFailureReason.THROTTLED,
        )

        private val DSN_STATUS = Regex("Status:\\s*(\\d(?:\\.\\d{1,3}){2})", RegexOption.IGNORE_CASE)
        private val DSN_FINAL_RECIPIENT =
            Regex("Final-Recipient:\\s*(?:[^;\\s]+\\s*;\\s*)?(\\S+)", RegexOption.IGNORE_CASE)
        private val DSN_DIAGNOSTIC =
            Regex("Diagnostic-Code:\\s*((?:[^\\r\\n]|\\r?\\n[ \\t])*)", RegexOption.IGNORE_CASE)

        private val UNKNOWN_RECIPIENT_STATUSES = setOf("5.1.0", "5.1.1")
        private val MALICIOUS = Regex("malicious|corrupted", RegexOption.IGNORE_CASE)
        private val UNKNOWN_RECIPIENT = Regex(
            "user unknown|no such user|does not accept mail|recipient address rejected",
            RegexOption.IGNORE_CASE,
        )
        private val KINDLE_ADDRESS = Regex("[\\w.+-]+@kindle\\.com", RegexOption.IGNORE_CASE)
        private val BOUNCE_SUBJECTS = listOf(
            Regex("undelivered mail returned to sender", RegexOption.IGNORE_CASE),
            Regex("delivery status notification", RegexOption.IGNORE_CASE),
            Regex("mail delivery failed", RegexOption.IGNORE_CASE),
            Regex("returned to sender", RegexOption.IGNORE_CASE),
        )
    }
}
