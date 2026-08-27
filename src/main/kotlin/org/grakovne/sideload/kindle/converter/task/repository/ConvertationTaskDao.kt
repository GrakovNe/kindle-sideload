package org.grakovne.sideload.kindle.converter.task.repository

import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.generated.tables.ConvertationTask.Companion.CONVERTATION_TASK
import org.grakovne.sideload.kindle.generated.tables.records.ConvertationTaskRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class ConvertationTaskDao(
    private val dsl: DSLContext
) {

    fun save(task: ConvertationTask): ConvertationTask {
        dsl.insertInto(CONVERTATION_TASK)
            .set(CONVERTATION_TASK.ID, task.id)
            .set(CONVERTATION_TASK.USER_ID, task.userId)
            .set(CONVERTATION_TASK.SOURCE_FILE_URL, task.sourceFileUrl)
            .set(CONVERTATION_TASK.CREATED_AT, toDb(task.createdAt))
            .set(CONVERTATION_TASK.FAIL_REASON, task.failReason)
            .set(CONVERTATION_TASK.STATUS, task.status.name)
            .set(CONVERTATION_TASK.FILE_NAME, task.fileName)
            .onConflict(CONVERTATION_TASK.ID)
            .doUpdate()
            .set(CONVERTATION_TASK.FAIL_REASON, task.failReason)
            .set(CONVERTATION_TASK.STATUS, task.status.name)
            .execute()
        return task
    }

    fun findById(id: UUID): ConvertationTask? =
        dsl.selectFrom(CONVERTATION_TASK)
            .where(CONVERTATION_TASK.ID.eq(id))
            .fetchOne()
            ?.let { it.toDomain() }

    fun findByCreatedAtGreaterThanAndCreatedAtLessThan(
        from: Instant,
        to: Instant
    ): List<ConvertationTask> {
        return dsl.selectFrom(CONVERTATION_TASK)
            .where(CONVERTATION_TASK.CREATED_AT.gt(toDb(from)))
            .and(CONVERTATION_TASK.CREATED_AT.lt(toDb(to)))
            .fetch()
            .map { it.toDomain() }
    }

    fun findByStatusInAndCreatedAtLessThan(
        status: List<ConvertationTaskStatus>,
        lastModifiedAt: Instant
    ): List<ConvertationTask> {
        return dsl.selectFrom(CONVERTATION_TASK)
            .where(CONVERTATION_TASK.STATUS.`in`(status.map { it.name }))
            .and(CONVERTATION_TASK.CREATED_AT.lt(toDb(lastModifiedAt)))
            .fetch()
            .map { it.toDomain() }
    }

    fun findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(
        status: ConvertationTaskStatus,
        from: Instant,
        to: Instant
    ): List<ConvertationTask> {
        return dsl.selectFrom(CONVERTATION_TASK)
            .where(CONVERTATION_TASK.STATUS.eq(status.name))
            .and(CONVERTATION_TASK.CREATED_AT.gt(toDb(from)))
            .and(CONVERTATION_TASK.CREATED_AT.lt(toDb(to)))
            .fetch()
            .map { it.toDomain() }
    }

    fun saveAll(tasks: List<ConvertationTask>) = tasks.forEach { save(it) }

    fun findAll(): List<ConvertationTask> =
        dsl.selectFrom(CONVERTATION_TASK).fetch().map { it.toDomain() }

    fun count(): Int = dsl.fetchCount(CONVERTATION_TASK)

    fun deleteAll() = dsl.deleteFrom(CONVERTATION_TASK).execute()

    private fun toDb(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

    private fun fromDb(localDateTime: LocalDateTime): Instant =
        localDateTime.toInstant(ZoneOffset.UTC)

    private fun ConvertationTaskRecord.toDomain(): ConvertationTask =
        ConvertationTask(
            id = id!!,
            userId = userId!!,
            sourceFileUrl = sourceFileUrl!!,
            createdAt = fromDb(createdAt!!),
            failReason = failReason,
            status = ConvertationTaskStatus.valueOf(status!!),
            fileName = fileName
        )
}
