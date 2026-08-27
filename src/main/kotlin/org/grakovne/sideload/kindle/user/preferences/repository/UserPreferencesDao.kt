package org.grakovne.sideload.kindle.user.preferences.repository

import org.grakovne.sideload.kindle.generated.tables.UserPreferences.Companion.USER_PREFERENCES
import org.grakovne.sideload.kindle.generated.tables.records.UserPreferencesRecord
import org.grakovne.sideload.kindle.user.common.OutputFormat
import org.grakovne.sideload.kindle.user.preferences.domain.UserPreferences
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserPreferencesDao(
    private val dsl: DSLContext
) {

    fun save(preferences: UserPreferences): UserPreferences {
        dsl.insertInto(USER_PREFERENCES)
            .set(USER_PREFERENCES.ID, preferences.id)
            .set(USER_PREFERENCES.USER_ID, preferences.userId)
            .set(USER_PREFERENCES.OUTPUT_FORMAT, preferences.outputFormat.name)
            .set(USER_PREFERENCES.EMAIL, preferences.email)
            .set(USER_PREFERENCES.DEBUG_MODE, preferences.debugMode)
            .set(USER_PREFERENCES.AUTOMATIC_STK, preferences.automaticStk)
            .onConflict(USER_PREFERENCES.ID)
            .doUpdate()
            .set(USER_PREFERENCES.OUTPUT_FORMAT, preferences.outputFormat.name)
            .set(USER_PREFERENCES.EMAIL, preferences.email)
            .set(USER_PREFERENCES.DEBUG_MODE, preferences.debugMode)
            .set(USER_PREFERENCES.AUTOMATIC_STK, preferences.automaticStk)
            .execute()
        return preferences
    }

    fun findById(id: UUID): UserPreferences? =
        dsl.selectFrom(USER_PREFERENCES)
            .where(USER_PREFERENCES.ID.eq(id))
            .fetchOne()
            ?.let { it.toDomain() }

    fun findByUserId(userId: String): UserPreferences? =
        dsl.selectFrom(USER_PREFERENCES)
            .where(USER_PREFERENCES.USER_ID.eq(userId))
            .fetchOne()
            ?.let { it.toDomain() }

    fun saveAll(preferences: List<UserPreferences>) = preferences.forEach { save(it) }

    fun findAll(): List<UserPreferences> =
        dsl.selectFrom(USER_PREFERENCES).fetch().map { it.toDomain() }

    fun count(): Int = dsl.fetchCount(USER_PREFERENCES)

    fun deleteAll() = dsl.deleteFrom(USER_PREFERENCES).execute()

    private fun UserPreferencesRecord.toDomain(): UserPreferences {
        val id = requireNotNull(this.id) { "UserPreferences.id must be set by the database" }
        val userId = requireNotNull(this.userId) { "UserPreferences.user_id must be set by the database" }
        val outputFormat = requireNotNull(this.outputFormat) { "UserPreferences.output_format must be set by the database" }

        return UserPreferences(
            id = id,
            userId = userId,
            outputFormat = OutputFormat.valueOf(outputFormat),
            email = email,
            debugMode = debugMode ?: false,
            automaticStk = automaticStk ?: false
        )
    }
}
