package org.grakovne.sideload.kindle.notification.domain

enum class NotificationStatus {
    /** Waiting for its scheduled turn. */
    PENDING,

    /** Delivered to the chat. */
    SENT,

    /** The user cannot be reached at all (deleted the bot, blocked it, chat gone) — never retried. */
    UNDELIVERABLE,

    /** Gave up after exhausting the attempts. */
    FAILED,

    /** Superseded by a newer announcement before it was ever sent. */
    CANCELLED,
}
