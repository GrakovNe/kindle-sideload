package org.grakovne.sideload.kindle.telegram.handlers

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Document
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.FileUploadFailedError
import org.grakovne.sideload.kindle.common.configuration.FileUploadProperties
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.screens.convertation.BookConversionRequestHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.MainScreenRequestedEventHandler
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnprocessedIncomingEventServiceTest {

    private val messageSender: MessageWithNavigationSender = mock()
    private val stateService: UserActivityStateService = mock()
    private val buttonService: ButtonService = mock()
    private val convertationTaskService: ConvertationTaskService = mock()
    private val bot: TelegramBot = mock()
    private val fileUploadProperties: FileUploadProperties = mock()
    private val update: Update = mock()
    private val message: Message = mock()

    private var bookConversionProcessInvoked = false
    private lateinit var sut: UnprocessedIncomingEventService

    @BeforeEach
    fun setUp() {
        val mainScreen = MainScreenRequestedEventHandler(messageSender, buttonService, stateService)

        // Record the dispatch into the book-conversion handler without running its
        // real (network / bot / document) processEvent implementation.
        val bookConversion = object : BookConversionRequestHandler(
            convertationTaskService,
            messageSender,
            bot,
            fileUploadProperties,
            buttonService,
            stateService
        ) {
            override suspend fun processEvent(event: ButtonPressedEvent): Either<FileUploadFailedError, Unit> {
                bookConversionProcessInvoked = true
                return Either.Right(Unit)
            }
        }
        sut = UnprocessedIncomingEventService(mainScreen, bookConversion)
    }

    @Test
    fun `routs a document message to the book conversion handler`() {
        whenever(update.message()).thenReturn(message)
        whenever(message.document()).thenReturn(mock<Document>())
        val event = ButtonPressedEvent(update, User("user-1", "en", Type.FREE_USER, null))

        runBlocking { sut.handle(event) }

        assertTrue(bookConversionProcessInvoked, "book conversion handler should process a document message")
        verify(messageSender, never()).sendResponse(any<Update>(), any(), any(), any())
    }

    @Test
    fun `routs a plain message to the main screen handler`() {
        whenever(update.message()).thenReturn(message)
        whenever(message.document()).thenReturn(null)
        val event = ButtonPressedEvent(update, User("user-1", "en", Type.FREE_USER, null))

        runBlocking { sut.handle(event) }

        assertFalse(bookConversionProcessInvoked, "book conversion handler must not run without a document")
        verify(messageSender).sendResponse(any<Update>(), any(), any(), any())
    }
}
