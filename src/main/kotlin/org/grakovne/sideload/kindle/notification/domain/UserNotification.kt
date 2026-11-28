package org.grakovne.sideload.kindle.notification.domain

import java.time.Instant
import java.util.UUID

data class UserNotification(
    val id: UUID,
    val dedupKey: String,
    val userId: String,
    val kind: NotificationKind,
    val generation: Int,
    val payload: String?,
    val status: NotificationStatus,
    val attempts: Int,
    val createdAt: Instant,
    val nextAttemptAt: Instant?,
    val sentAt: Instant?,
)
