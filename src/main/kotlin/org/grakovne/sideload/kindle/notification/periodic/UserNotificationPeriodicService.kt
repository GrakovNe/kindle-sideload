package org.grakovne.sideload.kindle.notification.periodic

import mu.KotlinLogging
import org.grakovne.sideload.kindle.notification.configuration.NotificationProperties
import org.grakovne.sideload.kindle.notification.service.NotificationSendResult
import org.grakovne.sideload.kindle.notification.service.UserNotificationSender
import org.grakovne.sideload.kindle.notification.service.UserNotificationService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class UserNotificationPeriodicService(
    private val notificationService: UserNotificationService,
    private val notificationSender: UserNotificationSender,
    private val notificationProperties: NotificationProperties,
    private val clock: Clock,
) {

    /**
     * Sends whatever is due, never more than `max-per-tick`. The schedule already paces the
     * campaign, so this loop has nothing to hurry about; the limit is only a ceiling for a
     * mistyped spread.
     */
    @Scheduled(fixedDelay = TICK_DELAY_MS)
    fun sendDueNotifications() {
        if (!notificationProperties.enabled) {
            return
        }

        val due = notificationService.fetchDue(notificationProperties.maxPerTick, clock.instant())
        if (due.isEmpty()) {
            return
        }

        val results = due.map { notification ->
            runCatching { notificationSender.send(notification) }
                .onFailure { error -> logger.error(error) { "Unable to process notification ${notification.id}" } }
                .getOrNull()
        }

        val sent = results.count { it == NotificationSendResult.SENT }
        val undeliverable = results.count { it == NotificationSendResult.UNDELIVERABLE }
        val deferred = results.count { it == NotificationSendResult.DEFERRED }
        val failed = results.count { it == NotificationSendResult.FAILED || it == null }

        // One line per tick, and WARN only when something needs a human: per-message failures are
        // logged by the sender at INFO, and the appender would forward a WARN storm to super users.
        logger.info {
            "Notification tick: sent=$sent undeliverable=$undeliverable deferred=$deferred failed=$failed"
        }
        if (failed > 0) {
            logger.warn { "Notification tick had $failed failures, check the notification table" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private const val TICK_DELAY_MS = 5_000L
    }
}
