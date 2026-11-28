package org.grakovne.sideload.kindle.notification.web

import org.grakovne.sideload.kindle.notification.configuration.NotificationProperties
import org.grakovne.sideload.kindle.notification.domain.NotificationStatus
import org.grakovne.sideload.kindle.notification.service.SenderAddressAnnouncementService
import org.grakovne.sideload.kindle.notification.service.UserNotificationService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications/sender-address")
class SenderAddressAnnouncementEndpoint(
    private val announcementService: SenderAddressAnnouncementService,
    private val notificationService: UserNotificationService,
    private val properties: NotificationProperties,
    @Value("\${admin.token}")
    private val adminToken: String,
) {

    /**
     * Re-announces the configured sender address. Covers the case where the address was changed
     * but the app was not restarted, and lets the campaign be tested against a handful of chats.
     */
    @PostMapping("/announce")
    fun announce(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Map<String, Any?>> {
        if (!authorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val outcome = announcementService.forceAnnounce()
        val pending = notificationService.countByStatus(NotificationStatus.PENDING)

        return ResponseEntity.ok(
            mapOf(
                "started" to outcome.started,
                "address" to outcome.address,
                "generation" to outcome.generation,
                "enqueued" to outcome.enqueued,
                "pending" to pending,
                "campaignEndsAt" to outcome.campaignEndsAt?.toString(),
                "spreadOverHours" to properties.spreadOver.toHours(),
            ),
        )
    }

    private fun authorized(authorization: String?): Boolean {
        val token = adminToken
        return authorization == "Bearer $token"
    }
}
