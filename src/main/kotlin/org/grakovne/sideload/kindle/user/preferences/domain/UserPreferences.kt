package org.grakovne.sideload.kindle.user.preferences.domain

import org.grakovne.sideload.kindle.user.common.OutputFormat
import java.util.UUID

data class UserPreferences(
    val id: UUID,
    val userId: String,
    val outputFormat: OutputFormat,
    val email: String?,
    val debugMode: Boolean,
    val automaticStk: Boolean
)
