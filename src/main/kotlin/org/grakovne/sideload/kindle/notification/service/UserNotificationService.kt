package org.grakovne.sideload.kindle.notification.service

import org.grakovne.sideload.kindle.notification.domain.NotificationKind
import org.grakovne.sideload.kindle.notification.domain.NotificationStatus
import org.grakovne.sideload.kindle.notification.domain.UserNotification
import org.grakovne.sideload.kindle.notification.metrics.NotificationMetrics
import org.grakovne.sideload.kindle.notification.repository.UserNotificationDao
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class UserNotificationService(
    private val notificationDao: UserNotificationDao,
    private val metrics: NotificationMetrics,
    private val clock: Clock,
) {

    /**
     * Spreads [recipients] over [spreadOver] starting at [start] and enqueues them.
     *
     * The schedule is computed once, here, as absolute times: the campaign duration is then the
     * only thing anyone has to reason about, and a restart resumes on schedule instead of racing
     * to catch up. Deduplication is the unique dedup key, so re-running a campaign is a no-op.
     */
    fun enqueueCampaign(
        kind: NotificationKind,
        generation: Int,
        recipients: List<String>,
        payload: String,
        start: Instant,
        spreadOverSeconds: Long,
    ): Int {
        val cancelled = notificationDao.cancelPending(kind, generation)
        metrics.countCancelled(kind, cancelled)

        if (recipients.isEmpty()) {
            return 0
        }

        val intervalSeconds = spreadOverSeconds.toDouble() / recipients.size
        val now = clock.instant()

        val notifications = recipients.mapIndexed { index, userId ->
            UserNotification(
                id = UUID.randomUUID(),
                dedupKey = dedupKey(kind, generation, userId),
                userId = userId,
                kind = kind,
                generation = generation,
                payload = payload,
                status = NotificationStatus.PENDING,
                attempts = 0,
                createdAt = now,
                nextAttemptAt = start.plusSeconds((index * intervalSeconds).toLong()),
                sentAt = null,
            )
        }

        val enqueued = notificationDao.saveAllIfAbsent(notifications)
        metrics.countEnqueued(kind, enqueued)
        return enqueued
    }

    fun fetchDue(limit: Int, now: Instant): List<UserNotification> =
        notificationDao.fetchDue(limit, now)

    fun markSent(notification: UserNotification, sentAt: Instant) {
        if (notificationDao.markSent(notification.id, sentAt)) {
            metrics.countOutcome(notification.kind, NotificationStatus.SENT)
        }
    }

    fun markUndeliverable(notification: UserNotification) {
        if (notificationDao.markUndeliverable(notification.id)) {
            metrics.countOutcome(notification.kind, NotificationStatus.UNDELIVERABLE)
        }
    }

    fun markFailed(notification: UserNotification) {
        if (notificationDao.markFailed(notification.id)) {
            metrics.countOutcome(notification.kind, NotificationStatus.FAILED)
        }
    }

    fun scheduleRetry(notification: UserNotification, attempts: Int, nextAttemptAt: Instant) {
        notificationDao.scheduleRetry(notification.id, attempts, nextAttemptAt)
    }

    fun countByStatus(status: NotificationStatus): Int = notificationDao.countByStatus(status)

    companion object {
        fun dedupKey(kind: NotificationKind, generation: Int, userId: String): String =
            "${kind.name.lowercase()}:$generation:$userId"
    }
}
