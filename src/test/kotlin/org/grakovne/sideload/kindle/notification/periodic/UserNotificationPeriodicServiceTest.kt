package org.grakovne.sideload.kindle.notification.periodic

import org.grakovne.sideload.kindle.notification.configuration.NotificationProperties
import org.grakovne.sideload.kindle.notification.domain.NotificationKind
import org.grakovne.sideload.kindle.notification.domain.NotificationStatus
import org.grakovne.sideload.kindle.notification.domain.UserNotification
import org.grakovne.sideload.kindle.notification.service.NotificationSendResult
import org.grakovne.sideload.kindle.notification.service.UserNotificationSender
import org.grakovne.sideload.kindle.notification.service.UserNotificationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class UserNotificationPeriodicServiceTest {

    private val notificationService: UserNotificationService = mock()
    private val notificationSender: UserNotificationSender = mock()
    private val notificationProperties = NotificationProperties()

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private lateinit var sut: UserNotificationPeriodicService

    @BeforeEach
    fun setUp() {
        sut = UserNotificationPeriodicService(
            notificationService,
            notificationSender,
            notificationProperties,
            clock,
        )
    }

    @Test
    fun `sends nothing while notifications are disabled`() {
        notificationProperties.enabled = false

        sut.sendDueNotifications()

        verify(notificationService, never()).fetchDue(any(), any())
    }

    @Test
    fun `fetches at most max-per-tick notifications due right now`() {
        notificationProperties.enabled = true
        notificationProperties.maxPerTick = 2
        whenever(notificationService.fetchDue(2, now)).thenReturn(listOf(notification(), notification()))
        whenever(notificationSender.send(any())).thenReturn(NotificationSendResult.SENT)

        sut.sendDueNotifications()

        verify(notificationService).fetchDue(eq(2), eq(now))
        verify(notificationSender, times(2)).send(any())
    }

    @Test
    fun `keeps processing the rest of the batch when one notification throws`() {
        notificationProperties.enabled = true
        val throwing = notification()
        val healthy = notification()
        whenever(notificationService.fetchDue(any(), any())).thenReturn(listOf(throwing, healthy))
        whenever(notificationSender.send(throwing)).thenThrow(RuntimeException("telegram unreachable"))
        whenever(notificationSender.send(healthy)).thenReturn(NotificationSendResult.SENT)

        sut.sendDueNotifications()

        verify(notificationSender).send(healthy)
    }

    private fun notification() = UserNotification(
        id = UUID.randomUUID(),
        dedupKey = "sender_address_changed:1:${UUID.randomUUID()}",
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
