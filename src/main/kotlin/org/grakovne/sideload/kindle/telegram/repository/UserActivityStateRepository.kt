package org.grakovne.sideload.kindle.telegram.repository

import org.grakovne.sideload.kindle.telegram.domain.UserActivityState
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserActivityStateRepository : JpaRepository<UserActivityState, UUID> {
}