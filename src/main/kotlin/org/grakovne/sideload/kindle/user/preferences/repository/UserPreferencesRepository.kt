package org.grakovne.sideload.kindle.user.preferences.repository

import org.grakovne.sideload.kindle.user.preferences.domain.UserPreferences
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserPreferencesRepository : JpaRepository<UserPreferences, UUID> {

    fun findByUserId(userId: String): UserPreferences?
}