package org.grakovne.sideload.kindle.telegram.message.reference.domain

data class MessageReference(
    val id: String,
    val status: MessageStatus
)

enum class MessageStatus {
    UNKNOWN,
    PROCESSED
}
