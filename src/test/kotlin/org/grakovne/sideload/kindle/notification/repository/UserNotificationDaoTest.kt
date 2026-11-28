package org.grakovne.sideload.kindle.notification.repository

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.notification.domain.NotificationKind
import org.grakovne.sideload.kindle.notification.domain.NotificationStatus
import org.grakovne.sideload.kindle.notification.domain.UserNotification
import org.grakovne.sideload.kindle.notification.service.UserNotificationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.UUID

class UserNotificationDaoTest : TestDatabase() {

    @Autowired
    private lateinit var notificationDao: UserNotificationDao

    @Test
    fun `stores a notification and reads it back`() {
        val stored = notificationDao.saveIfAbsent(notification())

        assertTrue(stored)
        val fetched = notificationDao.fetchDue(10, Instant.now().plusSeconds(1)).single()
        assertEquals("user-1", fetched.userId)
        assertEquals(NotificationKind.SENDER_ADDRESS_CHANGED, fetched.kind)
        assertEquals(3, fetched.generation)
        assertEquals("new@test.local", fetched.payload)
        assertEquals(NotificationStatus.PENDING, fetched.status)
    }

    @Test
    fun `rejects a notification with the same dedup key`() {
        assertTrue(notificationDao.saveIfAbsent(notification()))

        val duplicate = notificationDao.saveIfAbsent(notification(id = UUID.randomUUID()))

        assertFalse(duplicate)
        assertEquals(1, notificationDao.findAll().size)
    }

    @Test
    fun `lets the same user through for a different generation`() {
        assertTrue(notificationDao.saveIfAbsent(notification(generation = 3)))
        assertTrue(notificationDao.saveIfAbsent(notification(id = UUID.randomUUID(), generation = 4)))

        assertEquals(2, notificationDao.findAll().size)
    }

    @Test
    fun `fetches only what is due, earliest first`() {
        notificationDao.saveIfAbsent(notification(id = UUID.randomUUID(), userId = "late", nextAttemptAt = at(60)))
        notificationDao.saveIfAbsent(notification(id = UUID.randomUUID(), userId = "early", nextAttemptAt = at(10)))
        notificationDao.saveIfAbsent(notification(id = UUID.randomUUID(), userId = "middle", nextAttemptAt = at(30)))

        val due = notificationDao.fetchDue(2, at(30))

        assertEquals(listOf("early", "middle"), due.map { it.userId })
        assertEquals(at(10), due.first().nextAttemptAt)
    }

    @Test
    fun `respects the limit`() {
        (1..5).forEach {
            notificationDao.saveIfAbsent(
                notification(id = UUID.randomUUID(), userId = "user-$it", nextAttemptAt = at(it.toLong()))
            )
        }

        assertEquals(2, notificationDao.fetchDue(2, at(100)).size)
    }

    @Test
    fun `settles a notification as sent and clears its slot`() {
        val notification = notification()
        notificationDao.saveIfAbsent(notification)

        assertTrue(notificationDao.markSent(notification.id, at(0)))

        val stored = notificationDao.findAll().single()
        assertEquals(NotificationStatus.SENT, stored.status)
        assertNull(stored.nextAttemptAt)
        assertEquals(at(0), stored.sentAt)
    }

    @Test
    fun `does not settle a notification twice`() {
        val notification = notification()
        notificationDao.saveIfAbsent(notification)

        assertTrue(notificationDao.markSent(notification.id, at(0)))
        assertFalse(notificationDao.markUndeliverable(notification.id))

        assertEquals(NotificationStatus.SENT, notificationDao.findAll().single().status)
    }

    @Test
    fun `marks a notification undeliverable`() {
        val notification = notification()
        notificationDao.saveIfAbsent(notification)

        assertTrue(notificationDao.markUndeliverable(notification.id))

        val stored = notificationDao.findAll().single()
        assertEquals(NotificationStatus.UNDELIVERABLE, stored.status)
        assertNull(stored.nextAttemptAt)
    }

    @Test
    fun `reschedules a retry keeping it pending`() {
        val notification = notification()
        notificationDao.saveIfAbsent(notification)

        assertTrue(notificationDao.scheduleRetry(notification.id, attempts = 2, nextAttemptAt = at(120)))

        val stored = notificationDao.findAll().single()
        assertEquals(NotificationStatus.PENDING, stored.status)
        assertEquals(2, stored.attempts)
        assertEquals(at(120), stored.nextAttemptAt)
    }

    @Test
    fun `does not resurrect a cancelled notification with retry bookkeeping`() {
        val notification = notification()
        notificationDao.saveIfAbsent(notification)
        notificationDao.cancelPending(NotificationKind.SENDER_ADDRESS_CHANGED, newerThanGeneration = 4)

        assertFalse(notificationDao.scheduleRetry(notification.id, attempts = 1, nextAttemptAt = at(120)))

        assertEquals(NotificationStatus.CANCELLED, notificationDao.findAll().single().status)
    }

    @Test
    fun `cancels pending notifications of older generations only`() {
        notificationDao.saveIfAbsent(notification(id = UUID.randomUUID(), generation = 3))
        notificationDao.saveIfAbsent(notification(id = UUID.randomUUID(), generation = 4))

        val cancelled = notificationDao.cancelPending(NotificationKind.SENDER_ADDRESS_CHANGED, newerThanGeneration = 4)

        assertEquals(1, cancelled)
        val stored = notificationDao.findAll()
        assertEquals(NotificationStatus.CANCELLED, stored.first { it.generation == 3 }.status)
        assertEquals(NotificationStatus.PENDING, stored.first { it.generation == 4 }.status)
    }

    @Test
    fun `leaves already sent notifications alone when cancelling`() {
        val sent = notification(id = UUID.randomUUID(), generation = 3)
        notificationDao.saveIfAbsent(sent)
        notificationDao.markSent(sent.id, at(0))

        assertEquals(0, notificationDao.cancelPending(NotificationKind.SENDER_ADDRESS_CHANGED, newerThanGeneration = 4))

        assertEquals(NotificationStatus.SENT, notificationDao.findAll().single().status)
    }

    @Test
    fun `counts notifications by status`() {
        notificationDao.saveIfAbsent(notification(id = UUID.randomUUID(), userId = "user-a"))
        notificationDao.saveIfAbsent(notification(id = UUID.randomUUID(), userId = "user-b", generation = 4))
        notificationDao.markUndeliverable(notificationDao.findAll().first { it.userId == "user-a" }.id)

        assertEquals(1, notificationDao.countByStatus(NotificationStatus.PENDING))
        assertEquals(1, notificationDao.countByStatus(NotificationStatus.UNDELIVERABLE))
        assertEquals(0, notificationDao.countByStatus(NotificationStatus.SENT))
    }

    private fun notification(
        id: UUID = UUID.randomUUID(),
        userId: String = "user-1",
        generation: Int = 3,
        nextAttemptAt: Instant? = at(0),
        dedupKey: String = UserNotificationService.dedupKey(NotificationKind.SENDER_ADDRESS_CHANGED, generation, userId),
    ) = UserNotification(
        id = id,
        dedupKey = dedupKey,
        userId = userId,
        kind = NotificationKind.SENDER_ADDRESS_CHANGED,
        generation = generation,
        payload = "new@test.local",
        status = NotificationStatus.PENDING,
        attempts = 0,
        createdAt = at(0),
        nextAttemptAt = nextAttemptAt,
        sentAt = null,
    )

    private fun at(offsetSeconds: Long) = Instant.parse("2026-01-01T00:00:00Z").plusSeconds(offsetSeconds)
}
