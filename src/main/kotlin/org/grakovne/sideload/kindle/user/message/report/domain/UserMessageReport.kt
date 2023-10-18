package org.grakovne.sideload.kindle.user.message.report.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Instant
import java.util.UUID

@Entity
data class UserMessageReport(
    @Id
    val id: UUID,
    val userId: String,
    val createdAt: Instant,
    val text: String?
)