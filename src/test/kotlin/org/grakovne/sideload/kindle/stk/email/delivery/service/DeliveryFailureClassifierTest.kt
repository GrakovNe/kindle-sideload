package org.grakovne.sideload.kindle.stk.email.delivery.service

import org.grakovne.sideload.kindle.stk.email.delivery.imap.RawEmailMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeliveryFailureClassifierTest {

    private val classifier = DeliveryFailureClassifier()

    // Amazon Send-to-Kindle problem notices

    @Test
    fun `clusters E001 as unsupported-format`() {
        val failure = classifier.classify(
            amazonNotice(
                """
                    We received the following document(s) you sent to Kindle:

                    * Книга — Тест.epub

                    Your document(s) could not be delivered due to E001 - Unsupported File Format:
                    The file format is not supported.
                """.trimIndent()
            )
        )

        assertEquals("unsupported-format", failure?.reason?.key)
        assertEquals("INBOX", failure?.imapFolder)
        assertEquals(101L, failure?.imapUid)
    }

    @Test
    fun `clusters E009 as no-attachment`() {
        val failure = classifier.classify(
            amazonNotice("Your document(s) could not be delivered due to E009 - No Attachment:")
        )

        assertEquals("no-attachment", failure?.reason?.key)
    }

    @Test
    fun `clusters E010 and E011 as throttled`() {
        val warning = classifier.classify(
            amazonNotice("Your document(s) could not be delivered due to E010 - Email Throttle Warning:")
        )
        val finalWarning = classifier.classify(
            amazonNotice("Your document(s) could not be delivered due to E011 - Email Throttle Final Warning:")
        )

        assertEquals("throttled", warning?.reason?.key)
        assertEquals("throttled", finalWarning?.reason?.key)
    }

    @Test
    fun `unrecognised amazon code becomes its own cluster`() {
        val failure = classifier.classify(
            amazonNotice(
                """
                    Your document(s) could not be delivered due to E999 - Send to Kindle Internal Error:
                    An internal error occurred. Please try again.
                """.trimIndent()
            )
        )

        assertEquals("E999", failure?.reason?.key)
        assertTrue(failure?.detail?.contains("E999") == true)
    }

    @Test
    fun `classifies amazon notice by body when subject is truncated`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 102L,
                subject = "Problem notice",
                body = "Your document(s) could not be delivered due to E009 - No Attachment:",
            )
        )

        assertEquals("no-attachment", failure?.reason?.key)
    }

    @Test
    fun `keeps the amazon error line as detail`() {
        val failure = classifier.classify(
            amazonNotice("Your document(s) could not be delivered due to E001 - Unsupported File Format:")
        )

        assertEquals(
            "could not be delivered due to E001 - Unsupported File Format:",
            failure?.detail
        )
    }

    @Test
    fun `classifies notice listing multiple files`() {
        val failure = classifier.classify(
            amazonNotice(
                """
                    We received the following document(s) you sent to Kindle:

                    * one.epub
                    * two.epub
                    * three.azw8

                    Your document(s) could not be delivered due to E001 - Unsupported File Format:
                """.trimIndent()
            )
        )

        assertEquals("unsupported-format", failure?.reason?.key)
        assertNull(failure?.recipient)
    }

    // Delivery Status Notifications

    @Test
    fun `clusters malicious content rejection and extracts recipient`() {
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

        assertEquals("malicious-content", failure?.reason?.key)
        assertEquals("si.simoonsi@kindle.com", failure?.recipient)
        assertTrue(failure?.detail?.contains("5.7.1") == true)
    }

    @Test
    fun `clusters recipient unknown status as unknown-recipient`() {
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

        assertEquals("unknown-recipient", failure?.reason?.key)
        assertEquals("ghost.reader@kindle.com", failure?.recipient)
    }

    @Test
    fun `clusters undefined recipient status as unknown-recipient`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 204,
                subject = "Returned to sender",
                body = """
                    Final-Recipient: rfc822; nobody@kindle.com
                    Action: failed
                    Status: 5.1.0
                """.trimIndent()
            )
        )

        assertEquals("unknown-recipient", failure?.reason?.key)
    }

    @Test
    fun `clusters unrecognised status code as itself`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 205,
                subject = "Delivery Status Notification (Failure)",
                body = """
                    Final-Recipient: rfc822; reader@kindle.com
                    Action: failed
                    Status: 5.7.234
                    Diagnostic-Code: smtp; 550 5.7.234 Something brand new happened
                """.trimIndent()
            )
        )

        assertEquals("5.7.234", failure?.reason?.key)
    }

    @Test
    fun `clusters timeout status as itself`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 206,
                subject = "Delivery Status Notification (Failure)",
                body = """
                    Delivery to the following recipient failed temporarily:

                    reader@kindle.com

                    Technical details: connection to smtp-in.amazon.com timed out

                    Final-Recipient: rfc822; reader@kindle.com
                    Action: delayed
                    Status: 4.4.7
                """.trimIndent()
            )
        )

        assertEquals("4.4.7", failure?.reason?.key)
        assertEquals("reader@kindle.com", failure?.recipient)
    }

    @Test
    fun `clusters throttle phrase inside dsn`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 207,
                subject = "Undelivered Mail Returned to Sender",
                body = """
                    Final-Recipient: rfc822; reader@kindle.com
                    Action: failed
                    Status: 4.2.1
                    Diagnostic-Code: smtp; 452 4.2.1 Too many messages from this sender, slow down
                """.trimIndent()
            )
        )

        assertEquals("throttled", failure?.reason?.key)
    }

    @Test
    fun `ignores successful delivery status notification`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 208,
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
    fun `extracts recipient without rfc822 prefix`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 209,
                subject = "Returned to sender",
                body = """
                    Final-Recipient: stray.reader@kindle.com
                    Action: failed
                    Status: 5.0.0
                """.trimIndent()
            )
        )

        assertEquals("stray.reader@kindle.com", failure?.recipient)
    }

    @Test
    fun `handles folded diagnostic code lines`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 210,
                subject = "Returned to sender",
                body = """
                    Final-Recipient: rfc822; reader@kindle.com
                    Action: failed
                    Status: 5.7.1
                    Diagnostic-Code: smtp; 550-5.7.1 This message does not meet the
                     recipient policy; 550 rejecting it entirely
                """.trimIndent()
            )
        )

        assertTrue(failure?.detail?.contains("recipient policy") == true)
    }

    @Test
    fun `truncates oversized detail`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 211,
                subject = "Returned to sender",
                body = """
                    Final-Recipient: rfc822; reader@kindle.com
                    Action: failed
                    Status: 5.7.1
                    Diagnostic-Code: smtp; 550 ${"x".repeat(600)}
                """.trimIndent()
            )
        )

        assertTrue((failure?.detail?.length ?: 0) <= 500)
    }

    @Test
    fun `matches status case insensitively`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 212,
                subject = "Returned to sender",
                body = """
                    Final-Recipient: rfc822; reader@kindle.com
                    status: 5.7.9
                """.trimIndent()
            )
        )

        assertEquals("5.7.9", failure?.reason?.key)
    }

    // Generic bounces and non-failures

    @Test
    fun `classifies subject-only bounce`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "Junk",
                uid = 301,
                subject = "Mail delivery failed: returning message to sender",
                body = "Sorry, the message could not be delivered.",
            )
        )

        assertEquals("bounce", failure?.reason?.key)
        assertEquals("Junk", failure?.imapFolder)
    }

    @Test
    fun `classifies undelivered mail subject without dsn fields`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 302,
                subject = "Undelivered Mail Returned to Sender",
                body = "The server was unable to deliver your message. reader@kindle.com",
            )
        )

        assertEquals("bounce", failure?.reason?.key)
        assertEquals("reader@kindle.com", failure?.recipient)
    }

    @Test
    fun `ignores regular messages`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 401,
                subject = "Your Goodreads Weekly",
                body = "New books from authors you follow. Status: great!",
            )
        )

        assertNull(failure)
    }

    @Test
    fun `ignores empty regular message`() {
        val failure = classifier.classify(
            RawEmailMessage(folder = "INBOX", uid = 402, subject = "", body = "")
        )

        assertNull(failure)
    }

    @Test
    fun `ignores apple account notices`() {
        val failure = classifier.classify(
            RawEmailMessage(
                folder = "INBOX",
                uid = 403,
                subject = "Завершите настройку пользовательского домена",
                body = "Ваш домен grakovne.org требует подтверждения.",
            )
        )

        assertNull(failure)
    }

    private fun amazonNotice(body: String) = RawEmailMessage(
        folder = "INBOX",
        uid = 101L,
        subject = "There was a problem with the document(s) you sent to Kindle",
        body = body,
    )
}
