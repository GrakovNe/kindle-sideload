package org.grakovne.sideload.kindle.notification.service

import org.grakovne.sideload.kindle.notification.domain.NotificationKind
import org.grakovne.sideload.kindle.notification.domain.NotificationStatus
import org.grakovne.sideload.kindle.notification.domain.UserNotification
import org.grakovne.sideload.kindle.notification.metrics.NotificationMetrics
import org.grakovne.sideload.kindle.notification.repository.UserNotificationDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

class UserNotificationServiceTest {

    private val notificationDao: UserNotificationDao = mock()
    private val metrics: NotificationMetrics = mock()

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private lateinit var sut: UserNotificationService

    @BeforeEach
    fun setUp() {
        sut = UserNotificationService(notificationDao, metrics, clock)
    }

    @Test
    fun `spreads the campaign over the whole window`() {
        whenever(notificationDao.saveAllIfAbsent(any())).thenReturn(4)

        sut.enqueueCampaign(
            kind = NotificationKind.SENDER_ADDRESS_CHANGED,
            generation = 1,
            recipients = listOf("u1", "u2", "u3", "u4"),
            payload = "new@test.local",
            start = now,
            spreadOverSeconds = Duration.ofHours(72).seconds,
        )

        val captor = argumentCaptor<List<UserNotification>>()
        verify(notificationDao).saveAllIfAbsent(captor.capture())
        val slots = captor.firstValue.map { it.nextAttemptAt!! }

        assertEquals(now, slots.first())
        // the last slot lands at the end of the window minus one interval, so the whole campaign
        // fits inside spread-over
        assertEquals(Duration.ofHours(72).seconds - Duration.ofHours(72).seconds / 4, slots.last().epochSecond - now.epochSecond)
        assertEquals(slots.distinct().size, slots.size)
    }

    @Test
    fun `stamps every notification with the dedup key of its generation and user`() {
        whenever(notificationDao.saveAllIfAbsent(any())).thenReturn(2)

        sut.enqueueCampaign(
            kind = NotificationKind.SENDER_ADDRESS_CHANGED,
            generation = 7,
            recipients = listOf("u1", "u2"),
            payload = "new@test.local",
            start = now,
            spreadOverSeconds = 100L,
        )

        val captor = argumentCaptor<List<UserNotification>>()
        verify(notificationDao).saveAllIfAbsent(captor.capture())

        assertEquals(
            listOf("sender_address_changed:7:u1", "sender_address_changed:7:u2"),
            captor.firstValue.map { it.dedupKey },
        )
    }

    @Test
    fun `cancels the previous generation before enqueuing a new one`() {
        whenever(notificationDao.cancelPending(NotificationKind.SENDER_ADDRESS_CHANGED, 7)).thenReturn(3)
        whenever(notificationDao.saveAllIfAbsent(any())).thenReturn(1)

        sut.enqueueCampaign(
            kind = NotificationKind.SENDER_ADDRESS_CHANGED,
            generation = 7,
            recipients = listOf("u1"),
            payload = "new@test.local",
            start = now,
            spreadOverSeconds = 100L,
        )

        verify(notificationDao).cancelPending(NotificationKind.SENDER_ADDRESS_CHANGED, 7)
        verify(metrics).countCancelled(NotificationKind.SENDER_ADDRESS_CHANGED, 3)
    }

    @Test
    fun `still cancels the previous generation when the new audience is empty`() {
        whenever(notificationDao.cancelPending(NotificationKind.SENDER_ADDRESS_CHANGED, 7)).thenReturn(5)

        sut.enqueueCampaign(
            kind = NotificationKind.SENDER_ADDRESS_CHANGED,
            generation = 7,
            recipients = emptyList(),
            payload = "new@test.local",
            start = now,
            spreadOverSeconds = 100L,
        )

        verify(notificationDao).cancelPending(NotificationKind.SENDER_ADDRESS_CHANGED, 7)
        verify(notificationDao, never()).saveAllIfAbsent(any())
    }

    @Test
    fun `counts only the rows that actually landed`() {
        whenever(notificationDao.saveAllIfAbsent(any())).thenReturn(1)

        sut.enqueueCampaign(
            kind = NotificationKind.SENDER_ADDRESS_CHANGED,
            generation = 1,
            recipients = listOf("u1", "u1"),
            payload = "new@test.local",
            start = now,
            spreadOverSeconds = 100L,
        )

        verify(metrics).countEnqueued(NotificationKind.SENDER_ADDRESS_CHANGED, 1)
    }

    @Test
    fun `settles a sent notification and counts it`() {
        val notification = notification()

        whenever(notificationDao.markSent(notification.id, now)).thenReturn(true)
        sut.markSent(notification, now)

        verify(metrics).countOutcome(NotificationKind.SENDER_ADDRESS_CHANGED, NotificationStatus.SENT)
    }

    @Test
    fun `does not count a send that lost the race against a cancellation`() {
        val notification = notification()

        whenever(notificationDao.markSent(notification.id, now)).thenReturn(false)
        sut.markSent(notification, now)

        verify(metrics, never()).countOutcome(any(), any())
    }

    private fun notification() = UserNotification(
        id = UUID.randomUUID(),
        dedupKey = "sender_address_changed:1:user-1",
        userId = "user-1",
        kind = NotificationKind.SENDER_ADDRESS_CHANGED,
        generation = 1,
        payload = "new@test.local",
        status = NotificationStatus.PENDING,
        attempts = 0,
        createdAt = now,
        nextAttemptAt = now,
        sentAt = null,
    )
}
