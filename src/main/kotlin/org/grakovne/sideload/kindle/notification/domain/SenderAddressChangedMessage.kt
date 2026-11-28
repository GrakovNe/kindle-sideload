package org.grakovne.sideload.kindle.notification.domain

import org.grakovne.sideload.kindle.common.navigation.domain.Message

/**
 * Tells a user that the address we send their books from is now [senderEmail], so they have to
 * whitelist it. The old address is deliberately not mentioned: it is already dead, and naming it
 * only invites users to keep the stale entry in their approved list.
 */
data class SenderAddressChangedMessage(
    val senderEmail: String,
) : Message
