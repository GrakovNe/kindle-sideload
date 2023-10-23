package org.grakovne.sideload.kindle.user.preferences.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import org.grakovne.sideload.kindle.user.common.OutputFormat
import java.util.UUID

@Entity
data class UserPreferences(
    @Id
    val id: UUID,
    val userId: String,
    @Enumerated(EnumType.STRING)
    val outputFormat: OutputFormat,
    val email: String?,
    val debugMode: Boolean
)