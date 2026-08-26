package org.grakovne.sideload.kindle.telegram.state.domain

import java.time.Instant
import java.util.UUID

data class UserActivityState(
    val id: UUID,
    val userId: String,
    val activityState: String,
    val createdAt: Instant
)
