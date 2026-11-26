package org.grakovne.sideload.kindle.stk.email.delivery.service

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.grakovne.sideload.kindle.stk.email.delivery.configuration.DeliveryMonitorProperties
import org.grakovne.sideload.kindle.stk.email.delivery.domain.DeliveryFailure
import org.grakovne.sideload.kindle.stk.email.delivery.imap.MailboxReader
import org.grakovne.sideload.kindle.stk.email.delivery.imap.RawEmailMessage
import org.grakovne.sideload.kindle.stk.email.delivery.metrics.DeliveryFailureMetrics
import org.grakovne.sideload.kindle.stk.email.delivery.repository.DeliveryFailureDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class DeliveryFailureMonitorServiceTest {

    private val mailboxReader = mock<MailboxReader>()
    private val deliveryFailureDao = mock<DeliveryFailureDao>()
    private val meterRegistry = SimpleMeterRegistry()
    private val properties = DeliveryMonitorProperties().apply { enabled = true }

    private lateinit var service: DeliveryFailureMonitorService

    @BeforeEach
    fun setUp() {
        service = DeliveryFailureMonitorService(
            properties,
            mailboxReader,
            DeliveryFailureClassifier(),
            deliveryFailureDao,
            DeliveryFailureMetrics(meterRegistry),
        )
    }

    @Test
    fun `stores classified failure and increments metric`() {
        deliver(maliciousBounce())
        whenever(deliveryFailureDao.saveIfAbsent(any())).thenReturn(true)

        service.poll()

        val captor = argumentCaptor<DeliveryFailure>()
        verify(deliveryFailureDao).saveIfAbsent(captor.capture())
        assertEquals("malicious-content", captor.firstValue.reason.key)
        assertEquals("si.simoonsi@kindle.com", captor.firstValue.recipient)
        assertEquals(1.0, counterValue("malicious-content"))
    }

    @Test
    fun `does not increment metric for already stored failure`() {
        deliver(maliciousBounce())
        whenever(deliveryFailureDao.saveIfAbsent(any())).thenReturn(false)

        service.poll()

        assertNull(counterValue("malicious-content"))
    }

    @Test
    fun `skips messages that are not delivery failures`() {
        deliver(RawEmailMessage("INBOX", 1L, "Your Goodreads Weekly", "New books from authors you follow"))

        service.poll()

        verifyNoInteractions(deliveryFailureDao)
        assertEquals(0, meterRegistry.meters.size)
    }

    @Test
    fun `does not touch mailbox when monitoring is disabled`() {
        properties.enabled = false

        service.poll()

        verifyNoInteractions(mailboxReader)
    }

    @Test
    fun `processes mixed batch of failures and regular mail`() {
        deliver(
            maliciousBounce(),
            RawEmailMessage("INBOX", 2L, "Invoice", "Your receipt is attached"),
            RawEmailMessage(
                folder = "INBOX",
                uid = 3L,
                subject = "There was a problem with the document(s) you sent to Kindle",
                body = "Your document(s) could not be delivered due to E001 - Unsupported File Format:",
            ),
        )
        whenever(deliveryFailureDao.saveIfAbsent(any())).thenReturn(true)

        service.poll()

        verify(deliveryFailureDao, times(2)).saveIfAbsent(any())
        assertEquals(1.0, counterValue("malicious-content"))
        assertEquals(1.0, counterValue("unsupported-format"))
    }

    @Test
    fun `acknowledges every message so the mailbox moves on`() {
        val handlerResults = mutableListOf<Boolean>()
        whenever(mailboxReader.fetchUnseen(any())).thenAnswer { invocation ->
            val handler = invocation.getArgument<(RawEmailMessage) -> Boolean>(0)
            handlerResults += handler(maliciousBounce())
            handlerResults += handler(RawEmailMessage("INBOX", 2L, "hi", "regular mail"))
        }

        service.poll()

        assertEquals(listOf(true, true), handlerResults)
    }

    private fun deliver(vararg messages: RawEmailMessage) {
        whenever(mailboxReader.fetchUnseen(any())).thenAnswer { invocation ->
            val handler = invocation.getArgument<(RawEmailMessage) -> Boolean>(0)
            messages.all { handler(it) }
        }
    }

    private fun counterValue(reason: String): Double? =
        meterRegistry.find(DeliveryFailureMetrics.FAILURE_METRIC)
            .tag("reason", reason)
            .counter()
            ?.count()

    private fun maliciousBounce() = RawEmailMessage(
        folder = "INBOX",
        uid = 77L,
        subject = "Undelivered Mail Returned to Sender",
        body = """
            Final-Recipient: rfc822; si.simoonsi@kindle.com
            Action: failed
            Status: 5.7.1
            Diagnostic-Code: smtp; 550 5.7.1 attachments may be corrupted or may contain malicious content
        """.trimIndent(),
    )
}
