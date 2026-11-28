package org.grakovne.sideload.kindle.notification.service

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.notification.configuration.NotificationProperties
import org.grakovne.sideload.kindle.notification.domain.SenderAddressChangedMessage
import org.grakovne.sideload.kindle.notification.domain.UserNotification
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.StkSettingsScreenButton
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.sender.SendFailure
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

/** What happened to one notification, so the worker can log a summary instead of per-message noise. */
enum class NotificationSendResult {
    SENT,
    UNDELIVERABLE,
    DEFERRED,
    FAILED,
}

@Service
class UserNotificationSender(
    private val messageSender: MessageWithNavigationSender,
    private val userService: UserService,
    private val notificationService: UserNotificationService,
    private val notificationProperties: NotificationProperties,
    private val clock: Clock,
) {

    /**
     * Renders [notification] in the language the user's profile carries and sends it. Every outcome
     * is terminal for this attempt: the notification either settles or gets a future slot, never
     * stays PENDING with nothing scheduled.
     */
    fun send(notification: UserNotification): NotificationSendResult {
        val address = notification.payload
            ?: return fail(notification, "notification has no payload")

        val user = try {
            userService.fetchUser(notification.userId)
        } catch (e: NoSuchElementException) {
            return fail(notification, "user is gone")
        }

        // The bot client wraps an IOException into a RuntimeException, so an unreachable Telegram
        // surfaces as a throw. It has to consume an attempt like any other failure, otherwise the
        // notification stays PENDING with a slot in the past and the worker retries it every tick.
        val outcome = runCatching {
            messageSender.sendNotification(
                notification.userId,
                user,
                SenderAddressChangedMessage(address),
                listOf(
                    listOf(StkSettingsScreenButton),
                    listOf(MainScreenButton),
                ),
            )
        }.getOrElse {
            logger.info { "Sending the notification to chat ${notification.userId} threw: ${it.message}" }
            return handleFailure(notification, SendFailure.Retryable)
        }

        return when (outcome) {
            is Either.Right -> {
                notificationService.markSent(notification, clock.instant())
                NotificationSendResult.SENT
            }

            is Either.Left -> handleFailure(notification, outcome.value)
        }
    }

    private fun handleFailure(
        notification: UserNotification,
        failure: SendFailure,
    ): NotificationSendResult = when (failure) {
        SendFailure.Undeliverable -> {
            notificationService.markUndeliverable(notification)
            NotificationSendResult.UNDELIVERABLE
        }

        is SendFailure.RateLimited -> {
            defer(notification, clock.instant().plus(failure.retryAfter))
            NotificationSendResult.DEFERRED
        }

        SendFailure.Retryable -> {
            if (notification.attempts + 1 >= notificationProperties.maxAttempts) {
                notificationService.markFailed(notification)
                NotificationSendResult.FAILED
            } else {
                defer(notification, clock.instant().plusSeconds(RETRY_BACKOFF_SECONDS))
                NotificationSendResult.DEFERRED
            }
        }
    }

    private fun defer(notification: UserNotification, nextAttemptAt: Instant) =
        notificationService.scheduleRetry(notification, notification.attempts + 1, nextAttemptAt)

    private fun fail(notification: UserNotification, reason: String): NotificationSendResult {
        logger.error { "Dropping notification ${notification.id} for user ${notification.userId}: $reason" }
        notificationService.markFailed(notification)
        return NotificationSendResult.FAILED
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private const val RETRY_BACKOFF_SECONDS = 300L
    }
}
