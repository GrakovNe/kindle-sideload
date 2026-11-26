package org.grakovne.sideload.kindle.delivery.imap

data class RawEmailMessage(
    val folder: String,
    val uid: Long,
    val subject: String,
    val body: String,
)

fun interface MailboxReader {

    /**
     * Hands every unseen message in the monitored mailbox to [handler] and marks
     * it seen only when the handler returns true. Returning false leaves the
     * message unseen so the next poll retries it.
     */
    fun fetchUnseen(handler: (RawEmailMessage) -> Boolean)
}
