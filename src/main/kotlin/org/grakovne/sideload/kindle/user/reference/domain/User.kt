package org.grakovne.sideload.kindle.user.reference.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "\"user\"")
data class User(
    @Id
    val id: String,
    val language: String?,
    @Enumerated(EnumType.STRING)
    val type: Type,
    val lastActivityTimestamp: Instant?
)

enum class Type {
    FREE_USER,
    SUPER_USER;
}