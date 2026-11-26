package org.grakovne.sideload.kindle.stk.email.delivery.service

import org.grakovne.sideload.kindle.stk.email.delivery.domain.DeliveryFailure
import org.grakovne.sideload.kindle.stk.email.delivery.domain.FailureReason
import org.grakovne.sideload.kindle.stk.email.delivery.imap.RawEmailMessage
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

/**
 * Recognises delivery-failure notices and clusters them by a reason key
 * derived from the message itself. Not a closed enum: an unrecognised
 * Amazon code or SMTP status becomes its own cluster.
 */
@Service
class DeliveryFailureClassifier {

    fun classify(message: RawEmailMessage): DeliveryFailure? {
        val haystack = "${message.subject}\n${message.body}"
        return amazonNotice(haystack, message)
            ?: dsnFailure(haystack, message)
            ?: genericBounce(message)
    }

    private fun amazonNotice(haystack: String, message: RawEmailMessage): DeliveryFailure? =
        (AMAZON_SUBJECT_ERROR.find(haystack) ?: AMAZON_BODY_ERROR.find(haystack))
            ?.let { match ->
                val code = match.groupValues[1]
                failure(
                    message = message,
                    reason = FailureReason(AMAZON_CLUSTERS[code] ?: code),
                    detail = AMAZON_ERROR_LINE.find(haystack)?.value?.trim(),
                )
            }

    private fun dsnFailure(haystack: String, message: RawEmailMessage): DeliveryFailure? =
        DSN_STATUS.find(haystack)
            ?.takeUnless { it.groupValues[1].startsWith("2") }
            ?.let { match ->
                val status = match.groupValues[1]
                failure(
                    message = message,
                    reason = FailureReason(dsnCluster(status, haystack)),
                    detail = dsnDetail(haystack),
                    recipient = DSN_FINAL_RECIPIENT.find(haystack)?.groupValues?.get(1),
                )
            }

    private fun dsnCluster(status: String, haystack: String): String = when {
        MALICIOUS_PHRASE.containsMatchIn(haystack) -> MALICIOUS_CLUSTER
        status in UNKNOWN_RECIPIENT_STATUSES || UNKNOWN_RECIPIENT_PHRASE.containsMatchIn(haystack) -> UNKNOWN_RECIPIENT_CLUSTER
        THROTTLED_PHRASE.containsMatchIn(haystack) -> THROTTLED_CLUSTER
        else -> status
    }

    private fun dsnDetail(haystack: String): String? =
        listOfNotNull(
            haystack.lineSequence().firstOrNull { it.contains("Status:") }?.trim(),
            DSN_DIAGNOSTIC.find(haystack)?.groupValues?.get(1)?.trim()?.let { "Diagnostic-Code: $it" },
        ).joinToString(" | ").ifBlank { null }

    private fun genericBounce(message: RawEmailMessage): DeliveryFailure? =
        BOUNCE_SUBJECTS
            .firstOrNull { it.containsMatchIn(message.subject) }
            ?.takeUnless { SUCCESS_DSN_STATUS.containsMatchIn(message.body) }
            ?.let { failure(message, FailureReason(BOUNCE_CLUSTER), message.subject.trim()) }

    private fun failure(
        message: RawEmailMessage,
        reason: FailureReason,
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

        private const val MALICIOUS_CLUSTER = "malicious-content"
        private const val UNKNOWN_RECIPIENT_CLUSTER = "unknown-recipient"
        private const val THROTTLED_CLUSTER = "throttled"
        private const val BOUNCE_CLUSTER = "bounce"

        private val AMAZON_CLUSTERS = mapOf(
            "E001" to "unsupported-format",
            "E009" to "no-attachment",
            "E010" to THROTTLED_CLUSTER,
            "E011" to THROTTLED_CLUSTER,
        )

        private val AMAZON_SUBJECT_ERROR = Regex(
            "problem with the document\\(s\\) you sent to Kindle[\\s\\S]*?due to (E\\d{3})",
            RegexOption.IGNORE_CASE,
        )
        private val AMAZON_BODY_ERROR =
            Regex("could not be delivered due to (E\\d{3})", RegexOption.IGNORE_CASE)
        private val AMAZON_ERROR_LINE = Regex("could not be delivered due to E\\d{3}[^\\n]*")

        private val DSN_STATUS = Regex("Status:\\s*(\\d(?:\\.\\d{1,3}){2})", RegexOption.IGNORE_CASE)
        private val SUCCESS_DSN_STATUS = Regex("Status:\\s*2(?:\\.\\d{1,3}){2}", RegexOption.IGNORE_CASE)
        private val DSN_FINAL_RECIPIENT =
            Regex("Final-Recipient:\\s*(?:[^;\\s]+\\s*;\\s*)?(\\S+)", RegexOption.IGNORE_CASE)
        private val DSN_DIAGNOSTIC =
            Regex("Diagnostic-Code:\\s*((?:[^\\r\\n]|\\r?\\n[ \\t])*)", RegexOption.IGNORE_CASE)

        private val UNKNOWN_RECIPIENT_STATUSES = setOf("5.1.0", "5.1.1")
        private val MALICIOUS_PHRASE = Regex("malicious|corrupted", RegexOption.IGNORE_CASE)
        private val UNKNOWN_RECIPIENT_PHRASE = Regex(
            "user unknown|no such user|does not accept mail|recipient address rejected",
            RegexOption.IGNORE_CASE,
        )
        private val THROTTLED_PHRASE = Regex("throttl|too many messages|rate limit", RegexOption.IGNORE_CASE)
        private val KINDLE_ADDRESS = Regex("[\\w.+-]+@kindle\\.com", RegexOption.IGNORE_CASE)
        private val BOUNCE_SUBJECTS = listOf(
            Regex("undelivered mail returned to sender", RegexOption.IGNORE_CASE),
            Regex("delivery status notification", RegexOption.IGNORE_CASE),
            Regex("mail delivery failed", RegexOption.IGNORE_CASE),
            Regex("returned to sender", RegexOption.IGNORE_CASE),
        )
    }
}
