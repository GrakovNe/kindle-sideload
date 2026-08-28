package org.grakovne.sideload.kindle.stk.email.task.repository

import org.grakovne.sideload.kindle.generated.tables.TransferEmailTask.Companion.TRANSFER_EMAIL_TASK
import org.grakovne.sideload.kindle.generated.tables.records.TransferEmailTaskRecord
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTask
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTaskStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class TransferEmailTaskDao(
    private val dsl: DSLContext
) {

    fun save(task: TransferEmailTask): TransferEmailTask {
        dsl.insertInto(TRANSFER_EMAIL_TASK)
            .set(TRANSFER_EMAIL_TASK.ID, task.id)
            .set(TRANSFER_EMAIL_TASK.USER_ID, task.userId)
            .set(TRANSFER_EMAIL_TASK.ENVIRONMENT_ID, task.environmentId)
            .set(TRANSFER_EMAIL_TASK.CREATED_AT, toDb(task.createdAt))
            .set(TRANSFER_EMAIL_TASK.FAIL_REASON, task.failReason)
            .set(TRANSFER_EMAIL_TASK.STATUS, task.status.name)
            .onConflict(TRANSFER_EMAIL_TASK.ID)
            .doUpdate()
            .set(TRANSFER_EMAIL_TASK.USER_ID, task.userId)
            .set(TRANSFER_EMAIL_TASK.ENVIRONMENT_ID, task.environmentId)
            .set(TRANSFER_EMAIL_TASK.CREATED_AT, toDb(task.createdAt))
            .set(TRANSFER_EMAIL_TASK.FAIL_REASON, task.failReason)
            .set(TRANSFER_EMAIL_TASK.STATUS, task.status.name)
            .execute()
        return task
    }

    fun findById(id: UUID): TransferEmailTask? =
        dsl.selectFrom(TRANSFER_EMAIL_TASK)
            .where(TRANSFER_EMAIL_TASK.ID.eq(id))
            .fetchOne()
            ?.let { it.toDomain() }

    fun findByStatusInAndCreatedAtLessThan(
        status: List<TransferEmailTaskStatus>,
        lastModifiedAt: Instant
    ): List<TransferEmailTask> {
        return dsl.selectFrom(TRANSFER_EMAIL_TASK)
            .where(TRANSFER_EMAIL_TASK.STATUS.`in`(status.map { it.name }))
            .and(TRANSFER_EMAIL_TASK.CREATED_AT.lt(toDb(lastModifiedAt)))
            .fetch()
            .map { it.toDomain() }
    }

    fun findByUserIdAndCreatedAtBetween(
        userId: String,
        start: Instant,
        end: Instant
    ): List<TransferEmailTask> {
        return dsl.selectFrom(TRANSFER_EMAIL_TASK)
            .where(TRANSFER_EMAIL_TASK.USER_ID.eq(userId))
            .and(TRANSFER_EMAIL_TASK.CREATED_AT.ge(toDb(start)))
            .and(TRANSFER_EMAIL_TASK.CREATED_AT.le(toDb(end)))
            .fetch()
            .map { it.toDomain() }
    }

    fun findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(
        status: TransferEmailTaskStatus,
        from: Instant,
        to: Instant
    ): List<TransferEmailTask> {
        return dsl.selectFrom(TRANSFER_EMAIL_TASK)
            .where(TRANSFER_EMAIL_TASK.STATUS.eq(status.name))
            .and(TRANSFER_EMAIL_TASK.CREATED_AT.gt(toDb(from)))
            .and(TRANSFER_EMAIL_TASK.CREATED_AT.lt(toDb(to)))
            .fetch()
            .map { it.toDomain() }
    }

    fun findByCreatedAtGreaterThanAndCreatedAtLessThan(
        from: Instant,
        to: Instant
    ): List<TransferEmailTask> {
        return dsl.selectFrom(TRANSFER_EMAIL_TASK)
            .where(TRANSFER_EMAIL_TASK.CREATED_AT.gt(toDb(from)))
            .and(TRANSFER_EMAIL_TASK.CREATED_AT.lt(toDb(to)))
            .fetch()
            .map { it.toDomain() }
    }

    fun findByFailReasonIsNotNullAndCreatedAtGreaterThanAndCreatedAtLessThan(
        from: Instant,
        to: Instant
    ): List<TransferEmailTask> {
        return dsl.selectFrom(TRANSFER_EMAIL_TASK)
            .where(TRANSFER_EMAIL_TASK.FAIL_REASON.isNotNull)
            .and(TRANSFER_EMAIL_TASK.CREATED_AT.gt(toDb(from)))
            .and(TRANSFER_EMAIL_TASK.CREATED_AT.lt(toDb(to)))
            .fetch()
            .map { it.toDomain() }
    }

    fun saveAll(tasks: List<TransferEmailTask>) = tasks.forEach { save(it) }

    fun findAll(): List<TransferEmailTask> =
        dsl.selectFrom(TRANSFER_EMAIL_TASK).fetch().map { it.toDomain() }

    fun count(): Int = dsl.fetchCount(TRANSFER_EMAIL_TASK)

    fun deleteAll() = dsl.deleteFrom(TRANSFER_EMAIL_TASK).execute()

    private fun toDb(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

    private fun fromDb(localDateTime: LocalDateTime): Instant =
        localDateTime.toInstant(ZoneOffset.UTC)

    private fun TransferEmailTaskRecord.toDomain(): TransferEmailTask =
        TransferEmailTask(
            id = id!!,
            userId = userId!!,
            environmentId = environmentId!!,
            createdAt = fromDb(createdAt!!),
            failReason = failReason,
            status = TransferEmailTaskStatus.valueOf(status!!)
        )
}
