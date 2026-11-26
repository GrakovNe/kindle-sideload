package org.grakovne.sideload.kindle.stk.email.delivery.imap

import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.Properties

class MimeTextExtractorTest {

    @Test
    fun `extracts plain text body`() {
        val text = extract(
            """
            From: daemon@icloud.com
            Subject: Undelivered
            Content-Type: text/plain; charset=UTF-8

            Your message could not be delivered.
            """.trimIndent()
        )

        assertEquals("Your message could not be delivered.", text)
    }

    @Test
    fun `extracts only the text part of multipart alternative`() {
        val text = extract(
            """
            MIME-Version: 1.0
            Content-Type: multipart/alternative; boundary="b1"

            --b1
            Content-Type: text/plain; charset=UTF-8

            Plain body here
            --b1
            Content-Type: text/html; charset=UTF-8

            <p>Html body here</p>
            --b1--
            """.trimIndent()
        )

        assertEquals("Plain body here", text)
        assertFalse(text.contains("Html"))
    }

    @Test
    fun `walks nested multiparts`() {
        val text = extract(
            """
            MIME-Version: 1.0
            Content-Type: multipart/mixed; boundary="outer"

            --outer
            Content-Type: multipart/alternative; boundary="inner"

            --inner
            Content-Type: text/plain

            Nested plain
            --inner--
            --outer
            Content-Type: text/plain

            Second plain part
            --outer--
            """.trimIndent()
        )

        assertEquals("Nested plain\nSecond plain part", text)
    }

    @Test
    fun `decodes base64 body`() {
        val text = extract(
            """
            Content-Type: text/plain; charset=UTF-8
            Content-Transfer-Encoding: base64

            SGVsbG8sIEtpbmRsZSE=
            """.trimIndent()
        )

        assertEquals("Hello, Kindle!", text)
    }

    @Test
    fun `decodes quoted printable body`() {
        val text = extract(
            """
            Content-Type: text/plain; charset=UTF-8
            Content-Transfer-Encoding: quoted-printable

            Caf=C3=A9 =28menu=29
            """.trimIndent()
        )

        assertEquals("Café (menu)", text)
    }

    @Test
    fun `ignores attachments and embedded original message`() {
        val text = extract(
            """
            MIME-Version: 1.0
            Content-Type: multipart/mixed; boundary="b"

            --b
            Content-Type: text/plain

            Bounce summary text
            --b
            Content-Type: message/rfc822

            From: sender@example.com
            Subject: original mail

            Original body with Status: 5.7.1 inside
            --b
            Content-Type: application/octet-stream
            Content-Disposition: attachment; filename="book.epub"

            binary-ish attachment
            --b--
            """.trimIndent()
        )

        assertEquals("Bounce summary text", text)
    }

    @Test
    fun `returns empty string for message without text parts`() {
        val text = extract(
            """
            MIME-Version: 1.0
            Content-Type: text/html

            <p>only html</p>
            """.trimIndent()
        )

        assertEquals("", text)
    }

    private fun extract(raw: String): String =
        MimeTextExtractor.textOf(
            MimeMessage(Session.getInstance(Properties()), raw.byteInputStream(Charsets.UTF_8))
        )
}
