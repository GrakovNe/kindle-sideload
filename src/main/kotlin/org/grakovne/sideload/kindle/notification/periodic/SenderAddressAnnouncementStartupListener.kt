package org.grakovne.sideload.kindle.notification.periodic

import mu.KotlinLogging
import org.grakovne.sideload.kindle.notification.configuration.NotificationProperties
import org.grakovne.sideload.kindle.notification.service.SenderAddressAnnouncementService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class SenderAddressAnnouncementStartupListener(
    private val announcementService: SenderAddressAnnouncementService,
    private val notificationProperties: NotificationProperties,
) {

    /**
     * A sender address only changes with a redeploy, so startup is the moment the change becomes
     * real. Runs after the context is up, which also guarantees the Flyway migrations behind the
     * announcement table have been applied.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun announceSenderAddress() {
        if (!notificationProperties.enabled) {
            logger.info { "Notifications are disabled, skipping the sender address announcement" }
            return
        }

        runCatching { announcementService.announceIfNeeded() }
            .onFailure {
                // A failed announcement must not take the app down: users can still upload and
                // convert, and the admin endpoint can start the campaign by hand.
                logger.error { "Unable to announce the sender address: ${it.message}" }
            }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
