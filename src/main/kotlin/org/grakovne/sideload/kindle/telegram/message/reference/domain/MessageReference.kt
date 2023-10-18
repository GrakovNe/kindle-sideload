package org.grakovne.sideload.kindle.telegram.message.reference.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.util.UUID

@Entity
data class MessageReference(
    @Id
    val id: String,
    @Enumerated(EnumType.STRING)
    val status: MessageStatus
)

enum class MessageStatus {
    UNKNOWN,
    PROCESSED
}