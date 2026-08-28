package org.grakovne.sideload.kindle.converter.task.domain

import java.time.Instant
import java.util.UUID

data class ConvertationTask(
    val id: UUID,
    val userId: String,
    val sourceFileUrl: String,
    val createdAt: Instant,
    val failReason: String?,
    val status: ConvertationTaskStatus,
    val fileName: String?
)

enum class ConvertationTaskStatus {
    ACTIVE,
    SUCCESS,
    FAILED
}
