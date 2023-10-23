package org.grakovne.sideload.kindle.user.preferences.service

import org.grakovne.sideload.kindle.user.common.OutputFormat
import org.grakovne.sideload.kindle.user.preferences.domain.UserPreferences
import org.grakovne.sideload.kindle.user.preferences.repository.UserPreferencesRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserPreferencesService(
    private val repository: UserPreferencesRepository
) {

    fun fetchPreferences(userId: String) = fetchOrCreate(userId)

    fun updateOutputFormat(userId: String, outputFormat: OutputFormat) = fetchOrCreate(userId)
        .copy(outputFormat = outputFormat)
        .let { repository.save(it) }

    fun updateDebugMode(userId: String, debugMode: Boolean) = fetchOrCreate(userId)
        .copy(debugMode = debugMode)
        .let { repository.save(it) }

    fun updateEmail(userId: String, email: String) = fetchOrCreate(userId)
        .copy(email = email)
        .let { repository.save(it) }

    private fun fetchOrCreate(userId: String) = repository.findByUserId(userId) ?: createNew(userId)

    private fun createNew(userId: String) =
        UserPreferences(
            id = UUID.randomUUID(),
            userId = userId,
            outputFormat = OutputFormat.EPUB,
            email = null,
            debugMode = false
        ).let { repository.save(it) }
}