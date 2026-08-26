package org.grakovne.sideload.kindle.telegram.state.repository

import org.grakovne.sideload.kindle.generated.tables.UserActivityState.Companion.USER_ACTIVITY_STATE
import org.grakovne.sideload.kindle.generated.tables.records.UserActivityStateRecord
import org.grakovne.sideload.kindle.telegram.state.domain.UserActivityState
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class UserActivityStateDao(
    private val dsl: DSLContext
) {

    fun save(state: UserActivityState): UserActivityState {
        dsl.insertInto(USER_ACTIVITY_STATE)
            .set(USER_ACTIVITY_STATE.ID, state.id)
            .set(USER_ACTIVITY_STATE.USER_ID, state.userId)
            .set(USER_ACTIVITY_STATE.ACTIVITY_STATE, state.activityState)
            .set(USER_ACTIVITY_STATE.CREATED_AT, toDb(state.createdAt))
            .onConflict(USER_ACTIVITY_STATE.ID)
            .doUpdate()
            .set(USER_ACTIVITY_STATE.ACTIVITY_STATE, state.activityState)
            .execute()
        return state
    }

    fun findById(id: UUID): UserActivityState? {
        return dsl.selectFrom(USER_ACTIVITY_STATE)
            .where(USER_ACTIVITY_STATE.ID.eq(id))
            .fetchOne()
            ?.let { it.toDomain() }
    }

    fun findByUserIdOrderByCreatedAtDesc(userId: String): List<UserActivityState> {
        return dsl.selectFrom(USER_ACTIVITY_STATE)
            .where(USER_ACTIVITY_STATE.USER_ID.eq(userId))
            .orderBy(USER_ACTIVITY_STATE.CREATED_AT.desc())
            .fetch()
            .map { it.toDomain() }
    }

    fun saveAll(states: List<UserActivityState>) = states.forEach { save(it) }

    fun findAll(): List<UserActivityState> =
        dsl.selectFrom(USER_ACTIVITY_STATE).fetch().map { it.toDomain() }

    fun count(): Int = dsl.fetchCount(USER_ACTIVITY_STATE)

    fun deleteAll() = dsl.deleteFrom(USER_ACTIVITY_STATE).execute()

    private fun toDb(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

    private fun fromDb(localDateTime: LocalDateTime): Instant =
        localDateTime.toInstant(ZoneOffset.UTC)

    private fun UserActivityStateRecord.toDomain(): UserActivityState {
        return UserActivityState(
            id = id!!,
            userId = userId!!,
            activityState = activityState!!,
            createdAt = fromDb(createdAt!!)
        )
    }
}
