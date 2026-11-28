package org.grakovne.sideload.kindle.notification

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import org.grakovne.sideload.kindle.KindleSideloadApplication
import org.grakovne.sideload.kindle.NoOpScheduling
import org.grakovne.sideload.kindle.common.mail.EmailProperties
import org.grakovne.sideload.kindle.notification.configuration.NotificationProperties
import org.grakovne.sideload.kindle.notification.domain.NotificationKind
import org.grakovne.sideload.kindle.notification.domain.NotificationStatus
import org.grakovne.sideload.kindle.notification.domain.UserNotification
import org.grakovne.sideload.kindle.notification.periodic.UserNotificationPeriodicService
import org.grakovne.sideload.kindle.notification.repository.SenderAddressAnnouncementDao
import org.grakovne.sideload.kindle.notification.repository.UserNotificationDao
import org.grakovne.sideload.kindle.notification.service.SenderAddressAnnouncementService
import org.grakovne.sideload.kindle.notification.web.SenderAddressAnnouncementEndpoint
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The sender-address campaign end to end: audience selection, per-user localisation, the spread
 * over the configured window, and the admin re-announcement.
 *
 * Same harness as [org.grakovne.sideload.kindle.acceptance.AcceptanceScenarioTest]: real Spring
 * context, embedded PostgreSQL, real localisation, mocked Telegram transport. The campaign is
 * driven by calling the periodic worker directly with a short window, so no test sleeps.
 */
@SpringBootTest(classes = [KindleSideloadApplication::class, NoOpScheduling::class])
class SenderAddressAnnouncementAcceptanceTest {

    @MockitoBean
    private lateinit var bot: TelegramBot

    @Autowired
    private lateinit var announcementService: SenderAddressAnnouncementService

    @Autowired
    private lateinit var notificationPeriodicService: UserNotificationPeriodicService

    @Autowired
    private lateinit var notificationDao: UserNotificationDao

    @Autowired
    private lateinit var announcementDao: SenderAddressAnnouncementDao

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userPreferencesDao: UserPreferencesDao

    @Autowired
    private lateinit var emailProperties: EmailProperties

    @Autowired
    private lateinit var notificationProperties: NotificationProperties

    @Autowired
    private lateinit var endpoint: SenderAddressAnnouncementEndpoint

    private val sent = mutableListOf<SendMessage>()

    // the properties beans are singletons shared with every other test of this context, so the
    // campaign knobs are restored after each scenario
    private var originalFrom: String? = null

    @BeforeEach
    fun seed() {
        originalFrom = emailProperties.from
        notificationDao.deleteAll()
        announcementDao.deleteAll()
        userPreferencesDao.deleteAll()
        userDao.deleteAll()
        sent.clear()

        val ok: SendResponse = mock()
        whenever(ok.isOk).thenReturn(true)
        whenever(bot.execute(any<SendMessage>())).thenAnswer {
            sent.add(it.getArgument<SendMessage>(0))
            ok
        }

        // the campaign window is a test property, not a production one: 6s over 3 users = 2s apart
        notificationProperties.enabled = true
        notificationProperties.spreadOver = Duration.ofSeconds(6)
        notificationProperties.maxPerTick = 3
        emailProperties.from = "new-sender@test.local"

        // ru, en, and a language with no localized resource at all
        user("announcer-1", "ru", "reader-1@kindle.com")
        user("announcer-2", "en", "reader-2@kindle.com")
        user("announcer-3", "de", "reader-3@kindle.com")
        // not in the audience: no STK address configured
        user("announcer-4", "ru", null)
    }

    @AfterEach
    fun restoreCampaignKnobs() {
        emailProperties.from = originalFrom!!
        notificationProperties.enabled = false
        notificationProperties.spreadOver = Duration.ofHours(72)
        notificationProperties.maxPerTick = 3
    }

    @Test
    fun `announces the configured address to every user with an stk address`() {
        announcementService.announceIfNeeded()

        val notifications = notificationDao.findAll()
        assertEquals(listOf("announcer-1", "announcer-2", "announcer-3"), notifications.map { it.userId }.sorted())
        assertTrue(notifications.all { it.kind == NotificationKind.SENDER_ADDRESS_CHANGED })
        assertTrue(notifications.all { it.payload == "new-sender@test.local" })
        assertTrue(notifications.all { it.status == NotificationStatus.PENDING })
    }

    @Test
    fun `spreads the campaign over the configured window`() {
        announcementService.announceIfNeeded()

        val slots = notificationDao.findAll().map { it.nextAttemptAt!! }.sorted()
        val span = Duration.between(slots.first(), slots.last())

        // three recipients over 6s -> slots at 0s, 2s, 4s: the last one still inside the window
        assertEquals(Duration.ofSeconds(4), span)
    }

    @Test
    fun `delivers each notification once, in the language of the user profile`() {
        announcementService.announceIfNeeded()
        rewindCampaign()

        notificationPeriodicService.sendDueNotifications()

        assertEquals(3, sent.size)
        // the chat_id delegate on SendMessage is typed Long, but the audience ids are strings,
        // so read the raw parameter the request was built with
        val byChat: Map<String, String> =
            sent.associate { it.getParameters()["chat_id"].toString() to it.text }

        assertTrue(byChat.getValue("announcer-1").contains("Наш адрес для отправки книг изменился"))
        assertTrue(byChat.getValue("announcer-2").contains("The E-Mail we send your books from has changed"))
        // no localized resource for the profile language -> the default (English) template
        assertTrue(byChat.getValue("announcer-3").contains("The E-Mail we send your books from has changed"))

        assertTrue(byChat.values.all { it.contains("new-sender@test.local") })
        // the old address must never appear
        assertTrue(byChat.values.none { it.contains("booksend@grakovne.org") })

        assertEquals(3, notificationDao.countByStatus(NotificationStatus.SENT))
        assertEquals(0, notificationDao.countByStatus(NotificationStatus.PENDING))
    }

    @Test
    fun `does not announce the same address twice`() {
        announcementService.announceIfNeeded()
        val first = notificationDao.findAll()

        val second = announcementService.announceIfNeeded()

        assertTrue(!second.started)
        assertEquals(first.size, notificationDao.findAll().size)
    }

    @Test
    fun `a changed address replaces the running campaign`() {
        announcementService.announceIfNeeded()

        emailProperties.from = "even-newer@test.local"
        val outcome = announcementService.announceIfNeeded()

        assertTrue(outcome.started)
        val stored = notificationDao.findAll()
        // nobody gets two messages: the slots of the superseded address are cancelled outright
        assertEquals(3, stored.count { it.status == NotificationStatus.CANCELLED })
        assertEquals(3, stored.count { it.status == NotificationStatus.PENDING })
        assertTrue(
            stored.filter { it.status == NotificationStatus.PENDING }
                .all { it.payload == "even-newer@test.local" }
        )
    }

    @Test
    fun `admin endpoint re-announces the current address`() {
        announcementService.announceIfNeeded()
        notificationPeriodicService.sendDueNotifications()

        val response = endpoint.announce("Bearer test-admin-token")

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body!!
        assertEquals(true, body["started"])
        assertEquals("new-sender@test.local", body["address"])
        assertEquals(2, body["generation"])
        assertEquals(3, body["enqueued"])

        val pending = notificationDao.findAll().filter { it.status == NotificationStatus.PENDING }
        assertEquals(3, pending.size)
        assertNotNull(pending.first().nextAttemptAt)
    }

    @Test
    fun `admin endpoint rejects an unauthorized caller`() {
        val response = endpoint.announce("Bearer nope")

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(0, notificationDao.findAll().size)
    }

    private fun user(id: String, language: String?, email: String?) {
        userDao.save(User(id, language, Type.FREE_USER, null))
        userPreferencesDao.save(
            UserPreferences(
                id = UUID.randomUUID(),
                userId = id,
                outputFormat = OutputFormat.EPUB,
                email = email,
                debugMode = false,
                automaticStk = false,
            )
        )
    }

    /** Makes every slot of the campaign due now, so the whole spread can be drained in one tick. */
    private fun rewindCampaign() {
        val past = Instant.now().minusSeconds(1)
        notificationDao.findAll().forEach { notificationDao.scheduleRetry(it.id, 0, past) }
    }
}
