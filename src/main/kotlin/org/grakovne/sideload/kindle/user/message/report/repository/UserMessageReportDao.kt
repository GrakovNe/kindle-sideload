package org.grakovne.sideload.kindle.user.message.report.repository

import org.grakovne.sideload.kindle.generated.tables.UserMessageReport.Companion.USER_MESSAGE_REPORT
import org.grakovne.sideload.kindle.generated.tables.records.UserMessageReportRecord
import org.grakovne.sideload.kindle.user.message.report.domain.UserMessageReport
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class UserMessageReportDao(
    private val dsl: DSLContext
) {

    fun save(report: UserMessageReport): UserMessageReport {
        dsl.insertInto(USER_MESSAGE_REPORT)
            .set(USER_MESSAGE_REPORT.ID, report.id)
            .set(USER_MESSAGE_REPORT.USER_ID, report.userId)
            .set(USER_MESSAGE_REPORT.CREATED_AT, toDb(report.createdAt))
            .set(USER_MESSAGE_REPORT.TEXT, report.text)
            .onConflict(USER_MESSAGE_REPORT.ID)
            .doUpdate()
            .set(USER_MESSAGE_REPORT.TEXT, report.text)
            .execute()
        return report
    }

    fun findById(id: UUID): UserMessageReport? {
        return dsl.selectFrom(USER_MESSAGE_REPORT)
            .where(USER_MESSAGE_REPORT.ID.eq(id))
            .fetchOne()
            ?.let { it.toDomain() }
    }

    fun findByCreatedAtGreaterThanAndCreatedAtLessThan(
        from: Instant,
        to: Instant
    ): List<UserMessageReport> {
        return dsl.selectFrom(USER_MESSAGE_REPORT)
            .where(USER_MESSAGE_REPORT.CREATED_AT.gt(toDb(from)))
            .and(USER_MESSAGE_REPORT.CREATED_AT.lt(toDb(to)))
            .fetch()
            .map { it.toDomain() }
    }

    fun saveAll(reports: List<UserMessageReport>) = reports.forEach { save(it) }

    fun findAll(): List<UserMessageReport> =
        dsl.selectFrom(USER_MESSAGE_REPORT).fetch().map { it.toDomain() }

    fun count(): Int = dsl.fetchCount(USER_MESSAGE_REPORT)

    fun deleteAll() = dsl.deleteFrom(USER_MESSAGE_REPORT).execute()

    private fun toDb(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

    private fun fromDb(localDateTime: LocalDateTime): Instant =
        localDateTime.toInstant(ZoneOffset.UTC)

    private fun UserMessageReportRecord.toDomain(): UserMessageReport {
        return UserMessageReport(
            id = id!!,
            userId = userId!!,
            createdAt = fromDb(createdAt!!),
            text = text
        )
    }
}
