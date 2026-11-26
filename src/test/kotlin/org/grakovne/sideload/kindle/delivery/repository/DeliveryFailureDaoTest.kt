package org.grakovne.sideload.kindle.delivery.repository

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.delivery.domain.DeliveryFailure
import org.grakovne.sideload.kindle.delivery.domain.DeliveryFailureReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        val failure = failure(reason = DeliveryFailureReason.THROTTLED)

        assertTrue(deliveryFailureDao.saveIfAbsent(failure))

        val stored = deliveryFailureDao.findAll()
        assertEquals(1, stored.size)
        assertEquals(failure.id, stored.first().id)
        assertEquals(DeliveryFailureReason.THROTTLED, stored.first().reason)
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

    private fun failure(
        imapFolder: String = "INBOX",
        reason: DeliveryFailureReason = DeliveryFailureReason.BOUNCED,
    ) = DeliveryFailure(
        id = UUID.randomUUID(),
        imapFolder = imapFolder,
        imapUid = 42L,
        recipient = "si.simoonsi@kindle.com",
        reason = reason,
        detail = "550 attachments may be corrupted",
        detectedAt = LocalDateTime.now(),
    )
}
