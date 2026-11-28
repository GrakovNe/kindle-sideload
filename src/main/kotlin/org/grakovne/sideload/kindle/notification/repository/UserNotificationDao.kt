package org.grakovne.sideload.kindle.notification.repository

import org.grakovne.sideload.kindle.generated.tables.UserNotification.Companion.USER_NOTIFICATION
import org.grakovne.sideload.kindle.generated.tables.records.UserNotificationRecord
import org.grakovne.sideload.kindle.notification.domain.NotificationKind
import org.grakovne.sideload.kindle.notification.domain.NotificationStatus
import org.grakovne.sideload.kindle.notification.domain.UserNotification
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class UserNotificationDao(
    private val dsl: DSLContext
) {

    /** @return false when a notification with the same dedup key already exists. */
    fun saveIfAbsent(notification: UserNotification): Boolean =
        dsl.insertInto(USER_NOTIFICATION)
            .set(USER_NOTIFICATION.ID, notification.id)
            .set(USER_NOTIFICATION.DEDUP_KEY, notification.dedupKey)
            .set(USER_NOTIFICATION.USER_ID, notification.userId)
            .set(USER_NOTIFICATION.KIND, notification.kind.name)
            .set(USER_NOTIFICATION.GENERATION, notification.generation)
            .set(USER_NOTIFICATION.PAYLOAD, notification.payload)
            .set(USER_NOTIFICATION.STATUS, notification.status.name)
            .set(USER_NOTIFICATION.ATTEMPTS, notification.attempts)
            .set(USER_NOTIFICATION.CREATED_AT, toDb(notification.createdAt))
            .set(USER_NOTIFICATION.NEXT_ATTEMPT_AT, toDb(notification.nextAttemptAt))
            .onConflictDoNothing()
            .execute() > 0

    /** @return how many rows landed, so the caller can tell a fresh campaign from a replayed one. */
    fun saveAllIfAbsent(notifications: List<UserNotification>): Int =
        notifications.count { saveIfAbsent(it) }

    fun fetchDue(limit: Int, now: Instant): List<UserNotification> =
        dsl.selectFrom(USER_NOTIFICATION)
            .where(USER_NOTIFICATION.STATUS.eq(NotificationStatus.PENDING.name))
            .and(USER_NOTIFICATION.NEXT_ATTEMPT_AT.le(toDb(now)))
            .orderBy(USER_NOTIFICATION.NEXT_ATTEMPT_AT.asc())
            .limit(limit)
            .fetch()
            .map { it.toDomain() }

    fun markSent(id: UUID, sentAt: Instant): Boolean =
        dsl.update(USER_NOTIFICATION)
            .set(USER_NOTIFICATION.STATUS, NotificationStatus.SENT.name)
            .set(USER_NOTIFICATION.SENT_AT, toDb(sentAt))
            .set(USER_NOTIFICATION.NEXT_ATTEMPT_AT, null as LocalDateTime?)
            .where(USER_NOTIFICATION.ID.eq(id))
            .and(USER_NOTIFICATION.STATUS.eq(NotificationStatus.PENDING.name))
            .execute() > 0

    /** Terminal state for a user we can never reach — no further attempts. */
    fun markUndeliverable(id: UUID): Boolean = transition(id, NotificationStatus.UNDELIVERABLE)

    fun markFailed(id: UUID): Boolean = transition(id, NotificationStatus.FAILED)

    /**
     * Reschedules a retry. Guarded on PENDING so a notification cancelled while its send was in
     * flight is not resurrected by the retry bookkeeping.
     */
    fun scheduleRetry(id: UUID, attempts: Int, nextAttemptAt: Instant): Boolean =
        dsl.update(USER_NOTIFICATION)
            .set(USER_NOTIFICATION.ATTEMPTS, attempts)
            .set(USER_NOTIFICATION.NEXT_ATTEMPT_AT, toDb(nextAttemptAt))
            .where(USER_NOTIFICATION.ID.eq(id))
            .and(USER_NOTIFICATION.STATUS.eq(NotificationStatus.PENDING.name))
            .execute() > 0

    /** Drops everything a newer announcement supersedes; already-sent rows are left alone. */
    fun cancelPending(kind: NotificationKind, newerThanGeneration: Int): Int =
        dsl.update(USER_NOTIFICATION)
            .set(USER_NOTIFICATION.STATUS, NotificationStatus.CANCELLED.name)
            .set(USER_NOTIFICATION.NEXT_ATTEMPT_AT, null as LocalDateTime?)
            .where(USER_NOTIFICATION.STATUS.eq(NotificationStatus.PENDING.name))
            .and(USER_NOTIFICATION.KIND.eq(kind.name))
            .and(USER_NOTIFICATION.GENERATION.lt(newerThanGeneration))
            .execute()

    fun countByStatus(status: NotificationStatus): Int =
        dsl.fetchCount(USER_NOTIFICATION, USER_NOTIFICATION.STATUS.eq(status.name))

    fun findAll(): List<UserNotification> =
        dsl.selectFrom(USER_NOTIFICATION)
            .orderBy(USER_NOTIFICATION.NEXT_ATTEMPT_AT.asc())
            .fetch()
            .map { it.toDomain() }

    fun deleteAll() = dsl.deleteFrom(USER_NOTIFICATION).execute()

    private fun transition(id: UUID, status: NotificationStatus): Boolean =
        dsl.update(USER_NOTIFICATION)
            .set(USER_NOTIFICATION.STATUS, status.name)
            .set(USER_NOTIFICATION.NEXT_ATTEMPT_AT, null as LocalDateTime?)
            .where(USER_NOTIFICATION.ID.eq(id))
            .and(USER_NOTIFICATION.STATUS.eq(NotificationStatus.PENDING.name))
            .execute() > 0

    private fun toDb(instant: Instant?): LocalDateTime? =
        instant?.let { LocalDateTime.ofInstant(it, ZoneOffset.UTC) }

    private fun fromDb(localDateTime: LocalDateTime?): Instant? =
        localDateTime?.toInstant(ZoneOffset.UTC)

    private fun UserNotificationRecord.toDomain(): UserNotification =
        UserNotification(
            id = id!!,
            dedupKey = dedupKey!!,
            userId = userId!!,
            kind = NotificationKind.valueOf(kind!!),
            generation = generation ?: 0,
            payload = payload,
            status = NotificationStatus.valueOf(status!!),
            attempts = attempts ?: 0,
            createdAt = fromDb(createdAt)!!,
            nextAttemptAt = fromDb(nextAttemptAt),
            sentAt = fromDb(sentAt),
        )
}
