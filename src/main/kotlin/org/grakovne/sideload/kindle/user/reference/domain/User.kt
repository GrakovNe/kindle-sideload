package org.grakovne.sideload.kindle.user.reference.domain

import java.time.Instant
import java.util.*

data class User(
    val id: String,
    val language: String?,
    val type: Type,
    val lastActivityTimestamp: Instant?
)

enum class Type {
    FREE_USER,
    SUPER_USER;
}
