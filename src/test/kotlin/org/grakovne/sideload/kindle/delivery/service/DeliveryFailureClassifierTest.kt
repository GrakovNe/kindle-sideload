package org.grakovne.sideload.kindle.delivery.service

import org.grakovne.sideload.kindle.delivery.domain.DeliveryFailureReason
import org.grakovne.sideload.kindle.delivery.imap.RawEmailMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeliveryFailureClassifierTest {

    private val classifier = DeliveryFailureClassifier()

    @Test
    fun `classifies amazon unsupported format notice`() {
        val failure = classifier.classify(
            amazonNotice(
                "E001 - Unsupported File Format:",
                """
                    We received the following document(s) you sent to Kindle:

                    * Книга — Тест.epub

                    Your document(s) could not be delivered due to E001 - Unsupported File Format:
                    The file format is not supported.
                """.trimIndent()
            )
        )

        assertEquals(DeliveryFailureReason.UNSUPPORTED_FORMAT, failure?.reason)
        assertEquals("INBOX", failure?.imapFolder)
        assertEquals(101L, failure?.imapUid)
        assertTrue(failure?.detail?.contains("E001") == true)
    }

    @Test
    fun `classifies amazon no attachment notice`() {
        val failure = classifier.classify(
            amazonNotice(
                "E009 - No Attachment:",
                "Your document(s) could not be delivered due to E009 - No Attachment:"
            )
        )

        assertEquals(DeliveryFailureReason.NO_ATTACHMENT, failure?.reason)
    }

    @Test
    fun `classifies amazon throttle notices`() {
        val warning = classifier.classify(
            amazonNotice("E010 - Email Throttle Warning:", "due to E010 - Email Throttle Warning:")
        )
        val finalWarning = classifier.classify(
            amazonNotice("E011 - Email Throttle Final Warning:", "due to E011 - Email Throttle Final Warning:")
        )

        assertEquals(DeliveryFailureReason.THROTTLED, warning?.reason)
        assertEquals(DeliveryFailureReason.THROTTLED, finalWarning?.reason)
    }

    @Test
    fun `classifies unknown amazon error code`() {
        val failure = classifier.classify(
            amazonNotice(
                "E999 - Send to Kindle Internal Error:",
                """
                    Your document(s) could not be delivered due to E999 - Send to Kindle Internal Error:
                    An internal error occurred. Please try again.
                """.trimIndent()
            )
        )

        assertEquals(DeliveryFailureReason.UNKNOWN, failure?.reason)
        assertTrue(failure?.detail?.contains("E999") == true)
    }

    @Test
    fun `classifies malicious content bounce`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 202,
                subject = "Undelivered Mail Returned to Sender",
                body = """
                    This is the mail system at host icloud.com.

                    I'm sorry to have to inform you that your message could not
                    be delivered to one or more recipients.

                    The mail system

                    <si.simoonsi@kindle.com>: host smtp-in.amazon.com[54.239.54.11] said:
                    550 5.7.1 attachments may be corrupted or may contain malicious
                    content (in reply to end of DATA command)

                    Final-Recipient: rfc822; si.simoonsi@kindle.com
                    Original-Recipient: rfc822;si.simoonsi@kindle.com
                    Action: failed
                    Status: 5.7.1
                    Diagnostic-Code: smtp; 550 5.7.1 attachments may be corrupted or
                    may contain malicious content
                    Last-Attempt-Date: Fri, 27 Nov 2026 07:05:00 +0000
                """.trimIndent()
            )
        )

        assertEquals(DeliveryFailureReason.REJECTED_AS_MALICIOUS, failure?.reason)
        assertEquals("si.simoonsi@kindle.com", failure?.recipient)
        assertTrue(failure?.detail?.contains("5.7.1") == true)
    }

    @Test
    fun `classifies unknown recipient bounce`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 203,
                subject = "Undelivered Mail Returned to Sender",
                body = """
                    Final-Recipient: rfc822; ghost.reader@kindle.com
                    Action: failed
                    Status: 5.1.1
                    Diagnostic-Code: smtp; 550 5.1.1 <ghost.reader@kindle.com>:
                    Recipient address rejected: user unknown
                """.trimIndent()
            )
        )

        assertEquals(DeliveryFailureReason.UNKNOWN_RECIPIENT, failure?.reason)
        assertEquals("ghost.reader@kindle.com", failure?.recipient)
    }

    @Test
    fun `classifies generic bounce without known reason`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 204,
                subject = "Delivery Status Notification (Failure)",
                body = """
                    Delivery to the following recipient failed permanently:

                    reader@kindle.com

                    Technical details of failure: connection to smtp-in.amazon.com timed out

                    Final-Recipient: rfc822; reader@kindle.com
                    Action: failed
                    Status: 4.4.7
                """.trimIndent()
            )
        )

        assertEquals(DeliveryFailureReason.BOUNCED, failure?.reason)
        assertEquals("reader@kindle.com", failure?.recipient)
    }

    @Test
    fun `classifies bounce by subject when no dsn fields present`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "Junk",
                uid = 205,
                subject = "Mail delivery failed: returning message to sender",
                body = "Sorry, the message could not be delivered."
            )
        )

        assertEquals(DeliveryFailureReason.BOUNCED, failure?.reason)
        assertEquals("Junk", failure?.imapFolder)
    }

    @Test
    fun `ignores successful delivery notification`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 206,
                subject = "Delivery Status Notification (Success)",
                body = """
                    Final-Recipient: rfc822; happy.reader@kindle.com
                    Action: delivered
                    Status: 2.0.0
                """.trimIndent()
            )
        )

        assertNull(failure)
    }

    @Test
    fun `ignores regular messages`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 207,
                subject = "Your Goodreads Weekly",
                body = "New books from authors you follow. Status: great!"
            )
        )

        assertNull(failure)
    }

    private fun amazonNotice(errorLine: String, body: String) = RawEmailMessage(
        folder = "INBOX",
        uid = 101L,
        subject = "There was a problem with the document(s) you sent to Kindle",
        body = body + "\n" + errorLine,
    )
}
