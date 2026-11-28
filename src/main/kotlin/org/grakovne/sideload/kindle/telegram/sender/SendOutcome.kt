package org.grakovne.sideload.kindle.telegram.sender

import java.time.Duration

/**
 * Why a Telegram send failed, in terms of what the caller should do next.
 *
 * [UnableSendResponse] alone is not enough for outbound campaigns: retrying a user who deleted the
 * bot wastes attempts forever, and ignoring Telegram's `retry_after` on 429 is exactly what gets a
 * bot rate-limited harder.
 */
sealed interface SendFailure {
    /** The chat rejects us for good — the user deleted or blocked the bot, or the chat is gone. */
    data object Undeliverable : SendFailure

    /** Telegram asked us to back off for [retryAfter] (429). */
    data class RateLimited(val retryAfter: Duration) : SendFailure

    /** Anything else — worth another attempt. */
    data object Retryable : SendFailure
}
