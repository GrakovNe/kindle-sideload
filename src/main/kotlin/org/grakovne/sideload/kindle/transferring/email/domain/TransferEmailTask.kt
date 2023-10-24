package org.grakovne.sideload.kindle.transferring.email.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.time.Instant
import java.util.UUID

@Entity
data class TransferEmailTask(
    @Id
    val id: UUID,
    val userId: String,
    val environmentId: String,
    val createdAt: Instant,
    val failReason: String?,
    @Enumerated(EnumType.STRING)
    val status: TransferEmailTaskStatus
)

enum class TransferEmailTaskStatus {
    ACTIVE,
    SUCCESS,
    FAILED
}