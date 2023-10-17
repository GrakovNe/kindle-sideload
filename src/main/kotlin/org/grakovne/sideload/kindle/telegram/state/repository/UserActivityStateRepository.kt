package org.grakovne.sideload.kindle.telegram.state.repository

import org.grakovne.sideload.kindle.telegram.state.domain.UserActivityState
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserActivityStateRepository : JpaRepository<UserActivityState, UUID> {

    fun findByUserIdOrderByCreatedAtDesc(userId: String): List<UserActivityState>
}