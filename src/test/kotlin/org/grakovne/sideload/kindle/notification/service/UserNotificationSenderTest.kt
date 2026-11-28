package org.grakovne.sideload.kindle.notification.service

import arrow.core.Either
import org.grakovne.sideload.kindle.notification.configuration.NotificationProperties
import org.grakovne.sideload.kindle.notification.domain.NotificationKind
import org.grakovne.sideload.kindle.notification.domain.NotificationStatus
import org.grakovne.sideload.kindle.notification.domain.SenderAddressChangedMessage
import org.grakovne.sideload.kindle.notification.domain.UserNotification
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.sender.SendFailure
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import org.grakovne.sideload.kindle.common.navigation.domain.Message as DomainMessage

class UserNotificationSenderTest {

    private val messageSender: MessageWithNavigationSender = mock()
    private val userService: UserService = mock()
    private val notificationService: UserNotificationService = mock()

    private val notificationProperties = NotificationProperties().apply { maxAttempts = 3 }

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private lateinit var sut: UserNotificationSender

    @BeforeEach
    fun setUp() {
        sut = UserNotificationSender(
            messageSender,
            userService,
            notificationService,
            notificationProperties,
            clock,
        )
    }

    @Test
    fun `sends the notification in the language of the user profile`() {
        val notification = notification()
        whenever(userService.fetchUser("user-1")).thenReturn(user("ru"))
        whenever(messageSender.sendNotification(eq("user-1"), any(), any<DomainMessage>(), any()))
            .thenReturn(Either.Right(Unit))

        val result = sut.send(notification)

        assertEquals(NotificationSendResult.SENT, result)
        verify(messageSender).sendNotification(
            eq("user-1"),
            eq(user("ru")),
            eq(SenderAddressChangedMessage("new@test.local")),
            any(),
        )
        verify(notificationService).markSent(notification, now)
    }

    @Test
    fun `gives up on a user who deleted the bot`() {
        val notification = notification()
        whenever(userService.fetchUser("user-1")).thenReturn(user("en"))
        whenever(messageSender.sendNotification(any(), any(), any<DomainMessage>(), any()))
            .thenReturn(Either.Left(SendFailure.Undeliverable))

        val result = sut.send(notification)

        assertEquals(NotificationSendResult.UNDELIVERABLE, result)
        verify(notificationService).markUndeliverable(notification)
    }

    @Test
    fun `honours the retry_after of a rate limit`() {
        val notification = notification()
        whenever(userService.fetchUser("user-1")).thenReturn(user("en"))
        whenever(messageSender.sendNotification(any(), any(), any<DomainMessage>(), any()))
            .thenReturn(Either.Left(SendFailure.RateLimited(Duration.ofSeconds(42))))

        val result = sut.send(notification)

        assertEquals(NotificationSendResult.DEFERRED, result)
        verify(notificationService).scheduleRetry(notification, 1, now.plusSeconds(42))
    }

    @Test
    fun `retries a transient failure with a backoff`() {
        val notification = notification()
        whenever(userService.fetchUser("user-1")).thenReturn(user("en"))
        whenever(messageSender.sendNotification(any(), any(), any<DomainMessage>(), any()))
            .thenReturn(Either.Left(SendFailure.Retryable))

        val result = sut.send(notification)

        assertEquals(NotificationSendResult.DEFERRED, result)
        verify(notificationService).scheduleRetry(notification, 1, now.plusSeconds(300))
    }

    @Test
    fun `stops retrying once the attempts are exhausted`() {
        val notification = notification(attempts = 2)
        whenever(userService.fetchUser("user-1")).thenReturn(user("en"))
        whenever(messageSender.sendNotification(any(), any(), any<DomainMessage>(), any()))
            .thenReturn(Either.Left(SendFailure.Retryable))

        val result = sut.send(notification)

        assertEquals(NotificationSendResult.FAILED, result)
        verify(notificationService).markFailed(notification)
    }

    @Test
    fun `fails a notification whose user is gone`() {
        val notification = notification()
        whenever(userService.fetchUser("user-1")).thenThrow(NoSuchElementException("user-1 not found"))

        val result = sut.send(notification)

        assertEquals(NotificationSendResult.FAILED, result)
        verify(notificationService).markFailed(notification)
    }

    @Test
    fun `fails a notification without a payload`() {
        val notification = notification(payload = null)

        val result = sut.send(notification)

        assertEquals(NotificationSendResult.FAILED, result)
        verify(notificationService).markFailed(notification)
    }

    private fun user(language: String) = User("user-1", language, Type.FREE_USER, null)

    private fun notification(
        attempts: Int = 0,
        payload: String? = "new@test.local",
    ) = UserNotification(
        id = UUID.randomUUID(),
        dedupKey = "sender_address_changed:1:user-1",
        userId = "user-1",
        kind = NotificationKind.SENDER_ADDRESS_CHANGED,
        generation = 1,
        payload = payload,
        status = NotificationStatus.PENDING,
        attempts = attempts,
        createdAt = now,
        nextAttemptAt = now,
        sentAt = null,
    )
}
