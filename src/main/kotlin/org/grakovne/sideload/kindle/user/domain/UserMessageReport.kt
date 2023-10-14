package org.grakovne.sideload.kindle.user.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Instant
import java.util.*

@Entity
data class UserMessageReport (
    @Id
    val id: UUID,
    val userReferenceId: String,
    val createdAt: Instant,
    val text: String?
)