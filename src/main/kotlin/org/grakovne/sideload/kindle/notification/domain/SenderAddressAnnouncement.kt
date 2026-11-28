package org.grakovne.sideload.kindle.notification.domain

import java.time.Instant

/**
 * The sender address users have already been told about, plus how many times we announced one.
 * The generation belongs to the announcement rather than to the address: a forced re-announce of
 * the very same address must still reach users, and the dedup key is what makes "at most once"
 * hold — so the key carries the generation, and every announcement bumps it.
 */
data class SenderAddressAnnouncement(
    val id: String,
    val address: String?,
    val generation: Int,
    val announcedAt: Instant?,
) {
    companion object {
        const val SINGLETON_ID = "sender-address"
    }
}
