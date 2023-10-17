package org.grakovne.sideload.kindle.telegram.state.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.time.Instant
import java.util.UUID

@Entity
data class UserActivityState(
    @Id
    val id: UUID,
    val userId: String,
    @Enumerated(EnumType.STRING)
    val activityState: ActivityState?,
    val createdAt: Instant
)

enum class ActivityState {
    UPLOADING_CONFIGURATION_REQUESTED
}