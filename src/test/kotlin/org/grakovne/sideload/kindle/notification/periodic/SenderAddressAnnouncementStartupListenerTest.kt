package org.grakovne.sideload.kindle.notification.periodic

import org.grakovne.sideload.kindle.notification.configuration.NotificationProperties
import org.grakovne.sideload.kindle.notification.service.SenderAddressAnnouncementService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SenderAddressAnnouncementStartupListenerTest {

    private val announcementService: SenderAddressAnnouncementService = mock()
    private val notificationProperties = NotificationProperties()

    private val sut = SenderAddressAnnouncementStartupListener(announcementService, notificationProperties)

    @Test
    fun `announces on startup`() {
        notificationProperties.enabled = true

        sut.announceSenderAddress()

        verify(announcementService).announceIfNeeded()
    }

    @Test
    fun `does not announce while notifications are disabled`() {
        notificationProperties.enabled = false

        sut.announceSenderAddress()

        verify(announcementService, never()).announceIfNeeded()
    }

    @Test
    fun `a failed announcement does not break the startup`() {
        notificationProperties.enabled = true
        whenever(announcementService.announceIfNeeded()).thenThrow(RuntimeException("database went away"))

        sut.announceSenderAddress()
    }
}
