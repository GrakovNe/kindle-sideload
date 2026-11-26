package org.grakovne.sideload.kindle.stk.email.delivery.repository

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.stk.email.delivery.domain.DeliveryFailure
import org.grakovne.sideload.kindle.stk.email.delivery.domain.FailureReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

class DeliveryFailureDaoTest : TestDatabase() {

    @Autowired
    private lateinit var deliveryFailureDao: DeliveryFailureDao

    @Test
    fun `stores and retrieves delivery failure`() {
        val failure = failure(reason = FailureReason("throttled"))

        assertTrue(deliveryFailureDao.saveIfAbsent(failure))

        val stored = deliveryFailureDao.findAll()
        assertEquals(1, stored.size)
        assertEquals(failure.id, stored.first().id)
        assertEquals("throttled", stored.first().reason.key)
        assertEquals("si.simoonsi@kindle.com", stored.first().recipient)
        assertEquals("550 attachments may be corrupted", stored.first().detail)
    }

    @Test
    fun `ignores duplicates by folder and uid`() {
        val failure = failure()

        assertTrue(deliveryFailureDao.saveIfAbsent(failure))
        assertFalse(deliveryFailureDao.saveIfAbsent(failure.copy(id = UUID.randomUUID())))

        assertEquals(1, deliveryFailureDao.findAll().size)
    }

    @Test
    fun `keeps same uid from different folders`() {
        assertTrue(deliveryFailureDao.saveIfAbsent(failure()))
        assertTrue(deliveryFailureDao.saveIfAbsent(failure(imapFolder = "Junk")))

        assertEquals(2, deliveryFailureDao.findAll().size)
    }

    @Test
    fun `persists null recipient and detail`() {
        val failure = failure(recipient = null, detail = null)

        assertTrue(deliveryFailureDao.saveIfAbsent(failure))

        val stored = deliveryFailureDao.findAll().single()
        assertNull(stored.recipient)
        assertNull(stored.detail)
    }

    @Test
    fun `persists unrecognised reason keys`() {
        assertTrue(deliveryFailureDao.saveIfAbsent(failure(reason = FailureReason("5.7.234"))))

        assertEquals("5.7.234", deliveryFailureDao.findAll().single().reason.key)
    }

    @Test
    fun `orders failures by detected at descending`() {
        val older = failure(imapUid = 1L, detectedAt = LocalDateTime.now().minusDays(1))
        val newer = failure(imapUid = 2L, detectedAt = LocalDateTime.now())

        deliveryFailureDao.saveIfAbsent(older)
        deliveryFailureDao.saveIfAbsent(newer)

        assertEquals(listOf(2L, 1L), deliveryFailureDao.findAll().map { it.imapUid })
    }

    private fun failure(
        imapFolder: String = "INBOX",
        imapUid: Long = 42L,
        recipient: String? = "si.simoonsi@kindle.com",
        reason: FailureReason = FailureReason("malicious-content"),
        detail: String? = "550 attachments may be corrupted",
        detectedAt: LocalDateTime = LocalDateTime.now(),
    ) = DeliveryFailure(
        id = UUID.randomUUID(),
        imapFolder = imapFolder,
        imapUid = imapUid,
        recipient = recipient,
        reason = reason,
        detail = detail,
        detectedAt = detectedAt,
    )
}
