package org.grakovne.sideload.kindle.acceptance

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Document
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.File as TgFile
import com.pengrad.telegrambot.request.GetFile
import com.pengrad.telegrambot.request.SendDocument
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.GetFileResponse
import com.pengrad.telegrambot.response.SendResponse
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.KindleSideloadApplication
import org.grakovne.sideload.kindle.common.FileDownloadService
import org.grakovne.sideload.kindle.common.mail.MailSendingService
import org.grakovne.sideload.kindle.converter.ConversionResult
import org.grakovne.sideload.kindle.converter.ConverterService
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.StkLimitExhausted
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converter.task.periodic.ConvertSourceFilePeriodicService
import org.grakovne.sideload.kindle.converter.task.repository.ConvertationTaskDao
import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.metrics.api.domain.DailyMetrics
import org.grakovne.sideload.kindle.metrics.api.domain.UserDailyMetrics
import org.grakovne.sideload.kindle.metrics.web.MetricsEndpoint
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import org.grakovne.sideload.kindle.events.internal.UserEnvironmentUnnecessaryEvent
import org.grakovne.sideload.kindle.shelf.domain.ShelfItemStatus
import org.grakovne.sideload.kindle.shelf.repository.ShelfItemDao
import org.grakovne.sideload.kindle.shelf.service.ShelfService
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTask
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTaskStatus
import org.grakovne.sideload.kindle.stk.email.task.periodic.StkEmailPeriodicService
import org.grakovne.sideload.kindle.stk.email.task.repository.TransferEmailTaskDao
import org.grakovne.sideload.kindle.stk.email.task.service.TransferEmailTaskService
import org.grakovne.sideload.kindle.telegram.ConfigurationProperties
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.user.message.report.domain.UserMessageReport
import org.grakovne.sideload.kindle.user.message.report.repository.UserMessageReportDao
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.repository.UserDao
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.Trigger
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Acceptance scenarios AC-1 … AC-6 from TEST_PLAN.md.
 *
 * The full Spring context runs against the in-memory H2 database (Flyway migrations) with the real
 * event bus, services, repositories and localisation. Everything "outside" the application is
 * mocked so that no network connection is ever attempted:
 *
 *  - [TelegramBot] — the bot never polls (the library only does so on `getUpdates()`); outgoing
 *    `SendMessage`/`SendDocument` requests are captured here and answered with a success response;
 *  - [FileDownloadService] — the book bytes come from the test, no HTTP;
 *  - [ConverterService] — the `fb2c` binary / EPUB bypass are faked with a [ConversionResult];
 *  - [MailSendingService] — SMTP is never touched.
 *
 * The `@Scheduled` workers are neutralised with a no-op [TaskScheduler] (see [NoOpScheduling]):
 * the scenarios drive the periodic services manually, so nothing runs in the background and the
 * shared database stays deterministic.
 */
@SpringBootTest(classes = [KindleSideloadApplication::class, AcceptanceScenarioTest.NoOpScheduling::class])
class AcceptanceScenarioTest {

    @MockBean
    private lateinit var bot: TelegramBot

    @MockBean
    private lateinit var downloadService: FileDownloadService

    @MockBean
    private lateinit var converterService: ConverterService

    @MockBean
    private lateinit var mailSendingService: MailSendingService

    @Autowired
    private lateinit var taskService: ConvertationTaskService

    @Autowired
    private lateinit var convertationTaskRepository: ConvertationTaskDao

    @Autowired
    private lateinit var convertSourceFilePeriodicService: ConvertSourceFilePeriodicService

    @Autowired
    private lateinit var stkEmailTaskService: TransferEmailTaskService

    @Autowired
    private lateinit var stkEmailPeriodicService: StkEmailPeriodicService

    @Autowired
    private lateinit var transferEmailTaskRepository: TransferEmailTaskDao

    @Autowired
    private lateinit var userEnvironmentService: UserEnvironmentService

    @Autowired
    private lateinit var eventSender: EventSender

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userPreferencesService: UserPreferencesService

    @Autowired
    private lateinit var shelfService: ShelfService

    @Autowired
    private lateinit var shelfItemRepository: ShelfItemDao

    @Autowired
    private lateinit var configurationProperties: ConfigurationProperties

    @Autowired
    private lateinit var metricsEndpoint: MetricsEndpoint

    @Autowired
    private lateinit var userMessageReportRepository: UserMessageReportDao

    @Autowired
    private lateinit var userRepository: UserDao

    private val replies = mutableListOf<SendMessage>()
    private val documents = mutableListOf<SendDocument>()

    @BeforeEach
    fun captureBotRequests() {
        // every test shares the in-memory database; the periodic workers process all ACTIVE tasks,
        // so start each scenario from a clean queue to keep them independent of execution order
        convertationTaskRepository.deleteAll()
        transferEmailTaskRepository.deleteAll()
        shelfItemRepository.deleteAll()
        userMessageReportRepository.deleteAll()
        userRepository.deleteAll()

        replies.clear()
        documents.clear()
        val ok: SendResponse = mock()
        whenever(ok.isOk).thenReturn(true)
        whenever(bot.execute(any<SendMessage>())).thenAnswer {
            replies.add(it.getArgument<SendMessage>(0))
            ok
        }
        whenever(bot.execute(any<SendDocument>())).thenAnswer {
            documents.add(it.getArgument<SendDocument>(0))
            ok
        }
    }

    // AC-1 — Convert an FB2 book.
    @Test
    fun `AC-1 an fb2 book goes through the conversion pipeline and reaches the shelf and the user`() {
        val userId = "900001"
        val book = tempBook("ac1-book.fb2")
        val output = tempOutput("ac1-output.azw3")
        prepareUserAndShelf(userId)

        runBlocking { whenever(downloadService.download(any<String>(), any<String>())).thenReturn(book) }
        whenever(converterService.convertAndCollect(userId, book))
            .thenReturn(Either.Right(ConversionResult("all good", "ac1-env", listOf(output))))

        taskService.submitTask(userService.fetchUser(userId), "https://example.com/ac1-book.fb2", "ac1-book.fb2")
        convertSourceFilePeriodicService.convertSourceFiles()

        assertEquals(ConvertationTaskStatus.SUCCESS, taskOf(userId).status)
        assertEquals(ShelfItemStatus.ACTIVE, shelfItemRepository.findByEnvironmentId("ac1-env")?.status)
        assertTrue(sentFiles().any { it == output }, "the converted file must be sent to the user")
        assertTrue(
            replyTexts().any { it.contains("Conversion was successful") },
            "the user must receive the success message, got: ${replyTexts()}"
        )
    }

    // AC-2 — EPUB pass-through.
    @Test
    fun `AC-2 an epub book bypasses the converter and its output is reported unchanged`() {
        val userId = "900002"
        val book = tempBook("ac2-book.epub")
        val output = tempOutput("ac2-book.epub")
        prepareUserAndShelf(userId)

        runBlocking { whenever(downloadService.download(any<String>(), any<String>())).thenReturn(book) }
        whenever(converterService.convertAndCollect(userId, book))
            .thenReturn(Either.Right(ConversionResult("Bypass conversion completed: ac2-book.epub", "ac2-env", listOf(output))))

        taskService.submitTask(userService.fetchUser(userId), "https://example.com/ac2-book.epub", "ac2-book.epub")
        convertSourceFilePeriodicService.convertSourceFiles()

        assertEquals(ConvertationTaskStatus.SUCCESS, taskOf(userId).status)
        assertEquals(ShelfItemStatus.ACTIVE, shelfItemRepository.findByEnvironmentId("ac2-env")?.status)
        assertTrue(sentFiles().any { it == output }, "the passed-through epub must be sent to the user")
    }

    // AC-3 — Auto-STK on success.
    @Test
    fun `AC-3 automatic stk enqueues the transfer task and the stk worker mails it successfully`() {
        val userId = "900003"
        val book = tempBook("ac3-book.fb2")
        val output = tempOutput("ac3-output.azw3")
        prepareUserAndShelf(userId)
        userPreferencesService.updateAutomaticStk(userId, true)
        userPreferencesService.updateEmail(userId, "ac3@example.com")

        runBlocking { whenever(downloadService.download(any<String>(), any<String>())).thenReturn(book) }
        whenever(mailSendingService.sendFile(eq("ac3@example.com"), any<List<File>>()))
            .thenReturn(Either.Right(Unit))
        whenever(converterService.convertAndCollect(userId, book))
            .thenReturn(Either.Right(ConversionResult("all good", "ac3-env", listOf(output))))

        taskService.submitTask(userService.fetchUser(userId), "https://example.com/ac3-book.fb2", "ac3-book.fb2")
        convertSourceFilePeriodicService.convertSourceFiles()

        // the finished event was handled by the auto-stk handler → a transfer task was enqueued
        assertEquals(TransferEmailTaskStatus.ACTIVE, transferTaskOf(userId).status)
        assertEquals("ac3-env", transferTaskOf(userId).environmentId)

        stkEmailPeriodicService.stkEmail()

        assertEquals(TransferEmailTaskStatus.SUCCESS, transferTaskOf(userId).status)
        verify(mailSendingService).sendFile(eq("ac3@example.com"), any<List<File>>())
        assertTrue(
            replyTexts().any { it.contains("Files have been sent to E-Mail") },
            "the user must receive the stk success message, got: ${replyTexts()}"
        )
    }

    // AC-4 — STK daily limit.
    @Test
    fun `AC-4 exceeding the daily stk limit is rejected and the user receives the stk failure message`() {
        val userId = "900004"
        prepareUserAndShelf(userId)
        val previousLimit = configurationProperties.userStkDailyLimit
        configurationProperties.userStkDailyLimit = 1
        try {
            // the limit is enforced with a strict '>', so the third submission exceeds it
            stkEmailTaskService.submitTask(userId, "ac4-env-1")
            stkEmailTaskService.submitTask(userId, "ac4-env-2")
            val exceeded = stkEmailTaskService.submitTask(userId, "ac4-env-3")
            assertEquals(StkLimitExhausted, exceeded.swap().orNull())

            // the manual STK button (SendConvertedToEmailButton) hits the same limit and replies with the failure message
            eventSender.sendEvent(stkButtonEvent(userId, "ac4-env-3"))
            assertTrue(
                replyTexts().any { it.contains("Something went wrong, and the files couldn't be sent to E-Mail") },
                "the user must receive the stk failure message, got: ${replyTexts()}"
            )
        } finally {
            configurationProperties.userStkDailyLimit = previousLimit
        }
    }

    // AC-5 — Environment TTL.
    @Test
    fun `AC-5 an outdated environment is terminated and its shelf items are deactivated`() {
        val userId = "900005"
        val envId = "ac5-env"
        prepareUserAndShelf(userId)
        // materialise the environment folder and one deliverable inside it
        val folder = userEnvironmentService.provideTemporaryEnvironmentsFolder().toPath().resolve(envId).toFile()
        folder.mkdirs()
        File(folder, "stale.azw3").writeText("stale")

        // the finished-conversion event normally creates the ACTIVE shelf item, so drive the shelf
        // handler through the same event the periodic service would emit
        eventSender.sendEvent(
            ConvertationFinishedEvent(
                userId = userId,
                status = ConvertationFinishedStatus.SUCCESS,
                log = "all good",
                output = emptyList(),
                environmentId = envId
            )
        )
        assertEquals(ShelfItemStatus.ACTIVE, shelfItemRepository.findByEnvironmentId(envId)?.status)

        // the TTL worker emits this for every environment older than the configured TTL; APFS cannot
        // backdate creationTime, so the event is driven directly with the same payload
        eventSender.sendEvent(UserEnvironmentUnnecessaryEvent(envId))

        assertFalse(folder.exists(), "the environment folder must be terminated")
        assertEquals(ShelfItemStatus.TERMINATED, shelfItemRepository.findByEnvironmentId(envId)?.status)
    }

    // AC-6 — Conversation prompt → book upload.
    @Test
    fun `AC-6 pressing the prompt button then uploading a book submits a conversion task`() {
        val userId = "900006"
        prepareUserAndShelf(userId)

        // step 1: the prompt button sets the input-required activity state and replies with the prompt
        eventSender.sendEvent(promptEvent(userId, "en"))
        assertTrue(
            replyTexts().any { it.contains("Upload a book in fb2 format") },
            "the user must receive the conversation prompt, got: ${replyTexts()}"
        )

        // step 2: the uploaded document is resolved to a URL and the conversion task is submitted
        whenever(bot.execute(any<GetFile>())).thenAnswer {
            val file: TgFile = mock()
            whenever(file.fileId()).thenReturn("$userId-file-id")
            whenever(file.filePath()).thenReturn("remote/ac6-book.fb2")
            val response: GetFileResponse = mock()
            whenever(response.file()).thenReturn(file)
            response
        }
        whenever(bot.getFullFilePath(any<TgFile>()))
            .thenReturn("https://cdn.example.com/remote/ac6-book.fb2")
        eventSender.sendEvent(documentEvent(userId, "en", "ac6-book.fb2", 50))

        val task = taskOf(userId)
        assertEquals(ConvertationTaskStatus.ACTIVE, task.status)
        assertEquals("https://cdn.example.com/remote/ac6-book.fb2", task.sourceFileUrl)
        assertEquals("ac6-book.fb2", task.fileName)
        assertTrue(
            replyTexts().any { it.contains("File is being converted, it will be ready soon") },
            "the user must receive the conversion-requested message, got: ${replyTexts()}"
        )
    }

    // AC-7 — Daily metrics endpoint.
    @Test
    fun `AC-7 the daily metrics endpoint is protected by the admin bearer token and reports the day activity`() {
        // the scenarios share the in-memory database, so clear the whole history before counting the day
        convertationTaskRepository.deleteAll()
        userMessageReportRepository.deleteAll()
        userRepository.deleteAll()
        transferEmailTaskRepository.deleteAll()

        assertEquals(HttpStatus.UNAUTHORIZED, metricsEndpoint.dailyMetrics(metricsRequest(null)).statusCode)
        assertEquals(HttpStatus.UNAUTHORIZED, metricsEndpoint.dailyMetrics(metricsRequest("Bearer wrong-token")).statusCode)

        val userId = "900007"
        prepareUserAndShelf(userId)

        // one email delivered, one failed with a reason
        val emailOne = transferEmailTask(TransferEmailTaskStatus.SUCCESS, null)
        val emailTwo = transferEmailTask(TransferEmailTaskStatus.FAILED, "smtp down")
        transferEmailTaskRepository.saveAll(listOf(emailOne, emailTwo))

        // three raw messages from this user plus one from another user
        val msgOne = userMessage(userId)
        val msgTwo = userMessage(userId)
        val msgThree = userMessage(userId)
        val msgFour = userMessage("900008")
        userMessageReportRepository.saveAll(listOf(msgOne, msgTwo, msgThree, msgFour))

        // the conversion pipeline submitted and completed two tasks today: one success, one failure
        backdatedConvertationTask(ConvertationTaskStatus.SUCCESS, "ac7-ok.fb2", Duration.ofMinutes(45))
        backdatedConvertationTask(ConvertationTaskStatus.FAILED, "ac7-fail.fb2", Duration.ofMinutes(30))

        val response = metricsEndpoint.dailyMetrics(metricsRequest("Bearer test-admin-token"))

        assertEquals(HttpStatus.OK, response.statusCode)
        val metrics: DailyMetrics = response.body!!
        assertEquals(1, metrics.convertedBooks, "actual: $metrics")
        assertEquals(1, metrics.failedBooks, "actual: $metrics")
        assertEquals(1, metrics.sentEmails, "actual: $metrics")
        assertEquals(1, metrics.failedEmails, "actual: $metrics")
        assertEquals(
            listOf(
                UserDailyMetrics(userId, 3),
                UserDailyMetrics("900008", 1)
            ),
            metrics.users,
            "actual: $metrics"
        )

        // the endpoint refreshes the user activity, so the bot metrics see the user as active today
        val activeUser = userRepository.findById(userId)!!
        assertTrue(activeUser.lastActivityTimestamp != null)
        assertTrue(userService.fetchActiveUsers(Instant.now().minus(Duration.ofHours(1)), Instant.now()).map { it.id }.contains(userId))
    }

    // --------------------------------------------------------------------- helpers

    private fun metricsRequest(authorization: String?): MockHttpServletRequest {
        val request = MockHttpServletRequest()
        if (authorization != null) {
            request.addHeader("Authorization", authorization)
        }
        return request
    }

    private fun backdatedConvertationTask(status: ConvertationTaskStatus, fileName: String, ago: Duration) =
        convertationTaskRepository.save(
            ConvertationTask(
                id = UUID.randomUUID(),
                userId = "900007",
                sourceFileUrl = "https://example.com/$fileName",
                createdAt = Instant.now().minus(ago),
                failReason = if (status == ConvertationTaskStatus.FAILED) "converter exploded" else null,
                status = status,
                fileName = fileName
            )
        )

    private fun transferEmailTask(status: TransferEmailTaskStatus, failReason: String?) =
        transferEmailTaskRepository.save(
            TransferEmailTask(
                id = UUID.randomUUID(),
                userId = "900007",
                environmentId = "ac7-env",
                createdAt = Instant.now().minus(Duration.ofMinutes(45)),
                failReason = failReason,
                status = status
            )
        )

    private fun userMessage(userId: String) =
        userMessageReportRepository.save(
            UserMessageReport(
                id = UUID.randomUUID(),
                userId = userId,
                createdAt = Instant.now().minus(Duration.ofMinutes(20)),
                text = "text"
            )
        )

    private fun replyTexts(): List<String> = replies.mapNotNull { it.text }

    private fun sentFiles(): List<File> = documents.mapNotNull { it.contentFile }

    private fun taskOf(userId: String) = convertationTaskRepository.findAll().first { it.userId == userId }

    private fun transferTaskOf(userId: String) = transferEmailTaskRepository.findAll().first { it.userId == userId }

    private fun prepareUserAndShelf(userId: String) {
        userService.fetchOrCreateUser(userId, "en")
        // create the shelf up-front so the concurrently running finish handlers never contend on the
        // unique user_id when both the reply handler and the shelf handler call fetchOrCreateShelf
        shelfService.fetchOrCreateShelf(userId)
    }

    private fun tempBook(name: String): File =
        File.createTempFile("acceptance-book-", ".$name").apply { deleteOnExit(); writeText("book bytes") }

    private fun tempOutput(name: String): File =
        File.createTempFile("acceptance-output-", ".$name").apply { writeText("output bytes") }

    private fun promptEvent(userId: String, language: String): ButtonPressedEvent =
        buildEvent(userId, language, callbackData = "RequestConvertationPromptButton")

    private fun stkButtonEvent(userId: String, environmentId: String): ButtonPressedEvent =
        buildEvent(userId, "en", callbackData = "SendConvertedToEmailButton#$environmentId")

    private fun documentEvent(userId: String, language: String, fileName: String, size: Long): ButtonPressedEvent =
        buildEvent(userId, language, document = fileName to size)

    private fun buildEvent(
        userId: String,
        language: String,
        callbackData: String? = null,
        document: Pair<String, Long>? = null
    ): ButtonPressedEvent {
        val message: Message = mock()
        val chat: Chat = mock()
        whenever(chat.id()).thenReturn(userId.toLong())
        whenever(message.chat()).thenReturn(chat)

        document?.let { (name, size) ->
            val doc: Document = mock()
            whenever(doc.fileName()).thenReturn(name)
            whenever(doc.fileSize()).thenReturn(size)
            whenever(doc.fileId()).thenReturn("$userId-file-id")
            whenever(message.document()).thenReturn(doc)
        }

        val update: Update = mock()
        whenever(update.message()).thenReturn(message)
        callbackData?.let {
            val callback: CallbackQuery = mock()
            whenever(callback.data()).thenReturn(it)
            whenever(update.callbackQuery()).thenReturn(callback)
        }

        return ButtonPressedEvent(update, userService.fetchOrCreateUser(userId, language))
    }

    /**
     * A no-op scheduler: the `@EnableScheduling` post-processor schedules every `@Scheduled` method
     * onto the `taskScheduler` bean, so pointing it at a scheduler that never fires keeps the
     * periodic workers (the 100 ms converter, the 5 s STK and TTL workers) from running in the
     * background and corrupting the shared test state.
     */
    @Configuration
    open class NoOpScheduling {
        // a single parked task keeps the executor alive until Spring shuts it down at context close
        @Bean(destroyMethod = "shutdown")
        open fun neverFiringExecutor(): ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1)

        @Bean
        open fun taskScheduler(executor: ScheduledThreadPoolExecutor): TaskScheduler {
            val never: ScheduledFuture<*> = executor.schedule(Runnable {}, Long.MAX_VALUE, TimeUnit.MILLISECONDS)
            return object : TaskScheduler {
                override fun schedule(task: Runnable, trigger: Trigger): ScheduledFuture<*> = never
                override fun schedule(task: Runnable, startTime: Instant): ScheduledFuture<*> = never
                override fun scheduleAtFixedRate(task: Runnable, startTime: Instant, period: Duration): ScheduledFuture<*> = never
                override fun scheduleAtFixedRate(task: Runnable, period: Duration): ScheduledFuture<*> = never
                override fun scheduleWithFixedDelay(task: Runnable, startTime: Instant, delay: Duration): ScheduledFuture<*> = never
                override fun scheduleWithFixedDelay(task: Runnable, delay: Duration): ScheduledFuture<*> = never
            }
        }
    }
}
