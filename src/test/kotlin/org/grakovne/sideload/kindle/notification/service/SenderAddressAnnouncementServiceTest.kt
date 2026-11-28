package org.grakovne.sideload.kindle.notification.service

import org.grakovne.sideload.kindle.common.mail.EmailProperties
import org.grakovne.sideload.kindle.notification.configuration.NotificationProperties
import org.grakovne.sideload.kindle.notification.domain.NotificationKind
import org.grakovne.sideload.kindle.notification.domain.SenderAddressAnnouncement
import org.grakovne.sideload.kindle.notification.repository.SenderAddressAnnouncementDao
import org.grakovne.sideload.kindle.user.preferences.repository.UserPreferencesDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SenderAddressAnnouncementServiceTest {

    private val announcementDao: SenderAddressAnnouncementDao = mock()
    private val userPreferencesDao: UserPreferencesDao = mock()
    private val notificationService: UserNotificationService = mock()

    private val emailProperties = EmailProperties().apply { from = "new@test.local" }
    private val notificationProperties = NotificationProperties().apply {
        spreadOver = Duration.ofHours(72)
    }

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private lateinit var sut: SenderAddressAnnouncementService

    @BeforeEach
    fun setUp() {
        sut = SenderAddressAnnouncementService(
            announcementDao,
            userPreferencesDao,
            notificationService,
            emailProperties,
            notificationProperties,
            clock,
        )
    }

    @Test
    fun `announces on the very first run`() {
        whenever(announcementDao.find()).thenReturn(null)
        whenever(userPreferencesDao.findUserIdsWithEmail()).thenReturn(listOf("user-1", "user-2"))
        whenever(announcementDao.insertIfAbsent("new@test.local", 1, now)).thenReturn(true)
        whenever(notificationService.enqueueCampaign(any(), any(), any(), any(), any(), any())).thenReturn(2)

        val outcome = sut.announceIfNeeded()

        assertTrue(outcome.started)
        assertEquals(1, outcome.generation)
        assertEquals(2, outcome.enqueued)
        verify(notificationService).enqueueCampaign(
            kind = eq(NotificationKind.SENDER_ADDRESS_CHANGED),
            generation = eq(1),
            recipients = eq(listOf("user-1", "user-2")),
            payload = eq("new@test.local"),
            start = eq(now),
            spreadOverSeconds = eq(Duration.ofHours(72).seconds),
        )
    }

    @Test
    fun `does nothing when the address is already announced`() {
        whenever(announcementDao.find()).thenReturn(announcement(address = "new@test.local", generation = 5))

        val outcome = sut.announceIfNeeded()

        assertFalse(outcome.started)
        assertEquals(5, outcome.generation)
        verify(notificationService, never())
            .enqueueCampaign(any(), any(), any(), any(), any(), any())
        verify(announcementDao, never()).claimAddressChange(anyOrNull(), any(), any(), any())
    }

    @Test
    fun `announces everyone when the address changed`() {
        whenever(announcementDao.find()).thenReturn(announcement(address = "old@test.local", generation = 5))
        whenever(userPreferencesDao.findUserIdsWithEmail()).thenReturn(listOf("user-1"))
        whenever(announcementDao.claimAddressChange("old@test.local", "new@test.local", 6, now)).thenReturn(true)
        whenever(notificationService.enqueueCampaign(any(), any(), any(), any(), any(), any())).thenReturn(1)

        val outcome = sut.announceIfNeeded()

        assertTrue(outcome.started)
        assertEquals(6, outcome.generation)
        verify(notificationService).enqueueCampaign(
            kind = eq(NotificationKind.SENDER_ADDRESS_CHANGED),
            generation = eq(6),
            recipients = eq(listOf("user-1")),
            payload = eq("new@test.local"),
            start = eq(now),
            spreadOverSeconds = eq(Duration.ofHours(72).seconds),
        )
    }

    @Test
    fun `does not enqueue when another startup claimed the change`() {
        whenever(announcementDao.find()).thenReturn(announcement(address = "old@test.local", generation = 5))
        whenever(announcementDao.claimAddressChange("old@test.local", "new@test.local", 6, now)).thenReturn(false)

        val outcome = sut.announceIfNeeded()

        assertFalse(outcome.started)
        verify(notificationService, never())
            .enqueueCampaign(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `enqueues nobody when the audience is empty but still claims the address`() {
        whenever(announcementDao.find()).thenReturn(null)
        whenever(userPreferencesDao.findUserIdsWithEmail()).thenReturn(emptyList())
        whenever(announcementDao.insertIfAbsent("new@test.local", 1, now)).thenReturn(true)
        whenever(notificationService.enqueueCampaign(any(), any(), any(), any(), any(), any())).thenReturn(0)

        val outcome = sut.announceIfNeeded()

        assertTrue(outcome.started)
        assertEquals(0, outcome.enqueued)
    }

    @Test
    fun `force announce bumps the generation over the stored one`() {
        whenever(announcementDao.find()).thenReturn(announcement(address = "new@test.local", generation = 7))
        whenever(userPreferencesDao.findUserIdsWithEmail()).thenReturn(listOf("user-1"))
        whenever(announcementDao.bumpGeneration("new@test.local", 8, now)).thenReturn(true)
        whenever(notificationService.enqueueCampaign(any(), any(), any(), any(), any(), any())).thenReturn(1)

        val outcome = sut.forceAnnounce()

        assertTrue(outcome.started)
        assertEquals(8, outcome.generation)
        verify(notificationService)
            .enqueueCampaign(any(), eq(8), any(), eq("new@test.local"), eq(now), any())
    }

    @Test
    fun `force announce inserts the announcement row when there is none`() {
        whenever(announcementDao.find()).thenReturn(null)
        whenever(userPreferencesDao.findUserIdsWithEmail()).thenReturn(listOf("user-1"))
        whenever(announcementDao.insertIfAbsent("new@test.local", 1, now)).thenReturn(true)
        whenever(notificationService.enqueueCampaign(any(), any(), any(), any(), any(), any())).thenReturn(1)

        val outcome = sut.forceAnnounce()

        assertTrue(outcome.started)
        assertEquals(1, outcome.generation)
    }

    private fun announcement(address: String, generation: Int) = SenderAddressAnnouncement(
        id = SenderAddressAnnouncement.SINGLETON_ID,
        address = address,
        generation = generation,
        announcedAt = now,
    )
}
