package org.grakovne.sideload.kindle.notification.service

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.common.mail.EmailProperties
import org.grakovne.sideload.kindle.notification.repository.SenderAddressAnnouncementDao
import org.grakovne.sideload.kindle.user.common.OutputFormat
import org.grakovne.sideload.kindle.user.preferences.domain.UserPreferences
import org.grakovne.sideload.kindle.user.preferences.repository.UserPreferencesDao
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.repository.UserDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * The announcement row is what makes the campaign run exactly once, so it must not survive an
 * enqueue that failed: a claimed-but-not-enqueued address means the users were never told and,
 * because the address now looks announced, nothing will ever tell them.
 */
class SenderAddressAnnouncementAtomicityTest : TestDatabase() {

    @MockitoBean
    private lateinit var notificationService: UserNotificationService

    @Autowired
    private lateinit var announcementService: SenderAddressAnnouncementService

    @Autowired
    private lateinit var announcementDao: SenderAddressAnnouncementDao

    @Autowired
    private lateinit var userPreferencesDao: UserPreferencesDao

    @Autowired
    private lateinit var userRepository: UserDao

    @Autowired
    private lateinit var emailProperties: EmailProperties

    private var originalFrom: String = "sideload@test.local"

    @BeforeEach
    fun rememberSenderAddress() {
        originalFrom = emailProperties.from
    }

    @AfterEach
    fun restoreSenderAddress() {
        emailProperties.from = originalFrom
    }

    @Test
    fun `does not mark the address announced when the enqueue fails`() {
        userRepository.save(User("atomic-user", "ru", Type.FREE_USER, null))
        userPreferencesDao.save(
            UserPreferences(
                id = UUID.randomUUID(),
                userId = "atomic-user",
                outputFormat = OutputFormat.EPUB,
                email = "reader@kindle.com",
                debugMode = false,
                automaticStk = false,
            )
        )
        whenever(notificationService.enqueueCampaign(any(), any(), any(), anyOrNull(), any(), any()))
            .thenThrow(RuntimeException("database went away"))

        assertFailsWith<RuntimeException> { announcementService.announceIfNeeded() }

        assertNull(announcementDao.find(), "a failed enqueue must roll the claim back")
    }

    @Test
    fun `keeps the previous announcement when the address change cannot be enqueued`() {
        userRepository.save(User("atomic-user", "ru", Type.FREE_USER, null))
        userPreferencesDao.save(
            UserPreferences(
                id = UUID.randomUUID(),
                userId = "atomic-user",
                outputFormat = OutputFormat.EPUB,
                email = "reader@kindle.com",
                debugMode = false,
                automaticStk = false,
            )
        )
        announcementDao.insertIfAbsent("old@test.local", 1, Instant.now())
        emailProperties.from = "new@test.local"

        whenever(notificationService.enqueueCampaign(any(), any(), any(), anyOrNull(), any(), any()))
            .thenThrow(RuntimeException("database went away"))

        assertFailsWith<RuntimeException> { announcementService.announceIfNeeded() }

        val stored = announcementDao.find()
        assertEquals("old@test.local", stored?.address)
        assertEquals(1, stored?.generation)
    }
}
