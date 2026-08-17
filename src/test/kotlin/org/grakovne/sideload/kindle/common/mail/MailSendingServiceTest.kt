package org.grakovne.sideload.kindle.common.mail

import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mail.MailException
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MailSendingServiceTest {

    @TempDir
    lateinit var tempDir: File

    private val properties = EmailProperties().apply {
        from = "bot@example.com"
        subject = "Your book"
        text = "Please find the attachment"
    }

    private val mailSender: JavaMailSender = mock()
    private val sut = MailSendingService(properties, mailSender)

    private val message: MimeMessage = mock()

    @Test
    fun `sends the file as an attachment and reports success`() {
        whenever(mailSender.createMimeMessage()).thenReturn(message)

        val book = File(tempDir, "book.azw3").apply { writeText("fake azw3") }

        val result = sut.sendFile("kindle@example.com", listOf(book))

        assertTrue(result.isRight())
        verify(mailSender).createMimeMessage()
        verify(mailSender).send(message)
    }

    @Test
    fun `reports a delivery error when the sender fails`() {
        whenever(mailSender.createMimeMessage()).thenThrow(MailSendException("smtp is down"))

        val book = File(tempDir, "book.azw3").apply { writeText("fake azw3") }

        val result = sut.sendFile("kindle@example.com", listOf(book))

        assertTrue(result.isLeft())
        assertEquals(MailError.DELIVERY_ERROR, result.fold(ifLeft = { it }, ifRight = { throw AssertionError() }))
        verify(mailSender, never()).send(any<MimeMessage>())
    }

    @Test
    fun `sends without attachments when the list is empty`() {
        whenever(mailSender.createMimeMessage()).thenReturn(message)

        val result = sut.sendFile("kindle@example.com", emptyList())

        assertTrue(result.isRight())
        verify(mailSender).send(message)
    }
}
