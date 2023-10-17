package org.grakovne.sideload.kindle.converer.task.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.time.Instant
import java.util.*

@Entity
data class ConvertationTask(
    @Id
    val id: UUID,
    val userId: String,
    val sourceFileUrl: String,
    val createdAt: Instant,
    val failReason: String?,
    @Enumerated(EnumType.STRING)
    val status: ConvertationTaskStatus
)

enum class ConvertationTaskStatus {
    ACTIVE,
    SUCCESS,
    FAILED
}