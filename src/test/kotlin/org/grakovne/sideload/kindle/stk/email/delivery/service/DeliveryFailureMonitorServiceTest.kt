package org.grakovne.sideload.kindle.stk.email.delivery.service

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.grakovne.sideload.kindle.stk.email.delivery.configuration.DeliveryMonitorProperties
import org.grakovne.sideload.kindle.stk.email.delivery.domain.DeliveryFailure
import org.grakovne.sideload.kindle.stk.email.delivery.domain.EmailBaseline
import org.grakovne.sideload.kindle.stk.email.delivery.imap.MailboxReader
import org.grakovne.sideload.kindle.stk.email.delivery.imap.RawEmailMessage
import org.grakovne.sideload.kindle.stk.email.delivery.imap.ScanBaseline
import org.grakovne.sideload.kindle.stk.email.delivery.imap.ScanResult
import org.grakovne.sideload.kindle.stk.email.delivery.metrics.DeliveryFailureMetrics
import org.grakovne.sideload.kindle.stk.email.delivery.repository.DeliveryFailureDao
import org.grakovne.sideload.kindle.stk.email.delivery.repository.EmailBaselineDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class DeliveryFailureMonitorServiceTest {

    private val mailboxReader = mock<MailboxReader>()
    private val deliveryFailureDao = mock<DeliveryFailureDao>()
    private val emailBaselineDao = mock<EmailBaselineDao>()
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
            emailBaselineDao,
            DeliveryFailureMetrics(meterRegistry),
        )
        whenever(mailboxReader.scan(anyOrNull(), any()))
            .thenReturn(ScanResult("INBOX", 7L, 0L))
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
        verifyNoInteractions(emailBaselineDao)
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
    fun `reads from the stored baseline instead of the unseen flag`() {
        whenever(emailBaselineDao.find("INBOX"))
            .thenReturn(baseline(lastProcessedUid = 100L))

        service.poll()

        verify(mailboxReader).scan(eq(ScanBaseline(uidValidity = 7L, lastProcessedUid = 100L)), any())
    }

    @Test
    fun `advances the baseline after a scan`() {
        deliver(maliciousBounce())

        service.poll()

        val captor = argumentCaptor<EmailBaseline>()
        verify(emailBaselineDao).save(captor.capture())
        assertEquals("INBOX", captor.firstValue.imapFolder)
        assertEquals(7L, captor.firstValue.uidValidity)
        assertEquals(77L, captor.firstValue.lastProcessedUid)
    }

    @Test
    fun `does not store a baseline when nothing new was handled`() {
        whenever(mailboxReader.scan(anyOrNull(), any()))
            .thenReturn(ScanResult("INBOX", 7L, 42L))
        whenever(emailBaselineDao.find("INBOX"))
            .thenReturn(baseline(lastProcessedUid = 42L))

        service.poll()

        verify(emailBaselineDao, never()).save(any())
    }

    @Test
    fun `does not rewind the baseline`() {
        whenever(emailBaselineDao.find("INBOX"))
            .thenReturn(baseline(lastProcessedUid = 500L))
        whenever(mailboxReader.scan(anyOrNull(), any()))
            .thenReturn(ScanResult("INBOX", 7L, 3L))

        service.poll()

        verify(emailBaselineDao, never()).save(any())
    }

    @Test
    fun `stores the new position when the folder restarts its uid epoch`() {
        whenever(emailBaselineDao.find("INBOX"))
            .thenReturn(baseline(uidValidity = 7L, lastProcessedUid = 500L))
        whenever(mailboxReader.scan(anyOrNull(), any()))
            .thenReturn(ScanResult("INBOX", 8L, 3L))

        service.poll()

        val captor = argumentCaptor<EmailBaseline>()
        verify(emailBaselineDao).save(captor.capture())
        assertEquals(8L, captor.firstValue.uidValidity)
        assertEquals(3L, captor.firstValue.lastProcessedUid)
    }

    @Test
    fun `does not advance the baseline when a message fails to be stored`() {
        whenever(deliveryFailureDao.saveIfAbsent(any())).thenThrow(RuntimeException("database exploded"))
        whenever(mailboxReader.scan(anyOrNull(), any())).thenAnswer { invocation ->
            val handler = invocation.getArgument<(RawEmailMessage) -> Boolean>(1)
            handler(maliciousBounce(uid = 1L))
            ScanResult("INBOX", 7L, 1L)
        }

        assertThrows<RuntimeException> { service.poll() }

        verify(emailBaselineDao, never()).save(any())
    }

    private fun deliver(vararg messages: RawEmailMessage) {
        whenever(mailboxReader.scan(anyOrNull(), any())).thenAnswer { invocation ->
            val handler = invocation.getArgument<(RawEmailMessage) -> Unit>(1)
            messages.forEach(handler)
            ScanResult("INBOX", 7L, messages.maxOfOrNull { it.uid } ?: 0L)
        }
    }

    private fun baseline(
        uidValidity: Long = 7L,
        lastProcessedUid: Long = 0L,
    ) = EmailBaseline(
        imapFolder = "INBOX",
        uidValidity = uidValidity,
        lastProcessedUid = lastProcessedUid,
        updatedAt = LocalDateTime.now(),
    )

    private fun counterValue(reason: String): Double? =
        meterRegistry.find(DeliveryFailureMetrics.FAILURE_METRIC)
            .tag("reason", reason)
            .counter()
            ?.count()

    private fun maliciousBounce(uid: Long = 77L) = RawEmailMessage(
        folder = "INBOX",
        uid = uid,
        subject = "Undelivered Mail Returned to Sender",
        body = """
            Final-Recipient: rfc822; si.simoonsi@kindle.com
            Action: failed
            Status: 5.7.1
            Diagnostic-Code: smtp; 550 5.7.1 attachments may be corrupted or may contain malicious content
        """.trimIndent(),
    )
}
