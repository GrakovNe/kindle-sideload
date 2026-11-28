package org.grakovne.sideload.kindle.notification.service

import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.mail.EmailProperties
import org.grakovne.sideload.kindle.notification.configuration.NotificationProperties
import org.grakovne.sideload.kindle.notification.domain.NotificationKind
import org.grakovne.sideload.kindle.notification.repository.SenderAddressAnnouncementDao
import org.grakovne.sideload.kindle.user.preferences.repository.UserPreferencesDao
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

/** Outcome of a campaign start, for logging and for the admin endpoint response. */
data class AnnouncementOutcome(
    val started: Boolean,
    val address: String,
    val generation: Int,
    val enqueued: Int,
    val campaignEndsAt: Instant?,
)

@Service
class SenderAddressAnnouncementService(
    private val announcementDao: SenderAddressAnnouncementDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val notificationService: UserNotificationService,
    private val emailProperties: EmailProperties,
    private val notificationProperties: NotificationProperties,
    private val clock: Clock,
) {

    /**
     * Announces the configured sender address if it is not the one users were last told about.
     *
     * The very first run announces too — there is no "silent baseline" mode, because the address
     * users must whitelist is the one thing that must never be stale. What keeps that safe is the
     * spread, not suppression.
     */
    fun announceIfNeeded(): AnnouncementOutcome {
        val address = emailProperties.from
        val stored = announcementDao.find()
        val now = clock.instant()

        if (stored != null && stored.address == address) {
            logger.info { "Sender address $address is already announced, nothing to do" }
            return notStarted(address, stored.generation)
        }

        val generation = (stored?.generation ?: 0) + 1
        val claimed = when (stored) {
            null -> announcementDao.insertIfAbsent(address, generation, now)
            else -> announcementDao.claimAddressChange(stored.address, address, generation, now)
        }

        if (!claimed) {
            logger.info { "Sender address announcement to generation $generation was claimed elsewhere" }
            return notStarted(address, generation)
        }

        return enqueue(address, generation, now)
    }

    /** Re-announces the currently configured address, whatever the stored state says. */
    fun forceAnnounce(): AnnouncementOutcome {
        val address = emailProperties.from
        val stored = announcementDao.find()
        val now = clock.instant()
        val generation = (stored?.generation ?: 0) + 1

        val written = when (stored) {
            null -> announcementDao.insertIfAbsent(address, generation, now)
            else -> announcementDao.bumpGeneration(address, generation, now)
        }

        if (!written) {
            return notStarted(address, generation)
        }

        logger.warn { "Forcing a sender address re-announcement of $address as generation $generation" }
        return enqueue(address, generation, now)
    }

    private fun enqueue(address: String, generation: Int, now: Instant): AnnouncementOutcome {
        val recipients = userPreferencesDao.findUserIdsWithEmail()
        val spreadOver = notificationProperties.spreadOver
        val enqueued = notificationService.enqueueCampaign(
            kind = NotificationKind.SENDER_ADDRESS_CHANGED,
            generation = generation,
            recipients = recipients,
            payload = address,
            start = now,
            spreadOverSeconds = spreadOver.seconds,
        )

        val campaignEndsAt = now.plus(spreadOver)
        logger.info {
            "Announcing sender address $address as generation $generation to $enqueued users " +
                "over ${spreadOver.toHours()}h, finishing by $campaignEndsAt"
        }

        return AnnouncementOutcome(
            started = true,
            address = address,
            generation = generation,
            enqueued = enqueued,
            campaignEndsAt = campaignEndsAt,
        )
    }

    private fun notStarted(address: String, generation: Int) = AnnouncementOutcome(
        started = false,
        address = address,
        generation = generation,
        enqueued = 0,
        campaignEndsAt = null,
    )

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
