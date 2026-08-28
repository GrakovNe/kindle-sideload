package org.grakovne.sideload.kindle.stk.email.task.domain

import java.time.Instant
import java.util.UUID

data class TransferEmailTask(
    val id: UUID,
    val userId: String,
    val environmentId: String,
    val createdAt: Instant,
    val failReason: String?,
    val status: TransferEmailTaskStatus
)

enum class TransferEmailTaskStatus {
    ACTIVE,
    SUCCESS,
    FAILED
}
