package org.grakovne.sideload.kindle.user.message.report.domain

import java.time.Instant
import java.util.UUID

data class UserMessageReport(
    val id: UUID,
    val userId: String,
    val createdAt: Instant,
    val text: String?
)
