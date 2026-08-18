package org.grakovne.sideload.kindle.telegram.handlers.screens.convertation

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Document
import com.pengrad.telegrambot.model.File
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.GetFile
import com.pengrad.telegrambot.response.GetFileResponse
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.BookIsTooLargeError
import org.grakovne.sideload.kindle.common.TaskQueueingError
import org.grakovne.sideload.kindle.common.configuration.FileUploadProperties
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.common.navigation.domain.Message as DomainMessage
import org.grakovne.sideload.kindle.converter.FatalError
import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestConvertationPromptButton
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertationRequestedMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileUploadFailedMessage
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookConversionRequestHandlerTest {

    private val convertationTaskService: ConvertationTaskService = mock()
    private val messageSender: MessageWithNavigationSender = mock()
    private val bot: TelegramBot = mock()
    private val buttonService: ButtonService = mock()
    private val stateService: UserActivityStateService = mock()
    private val properties = FileUploadProperties().apply { maxSize = 100 }

    private val handler = BookConversionRequestHandler(
        convertationTaskService,
        messageSender,
        bot,
        properties,
        buttonService,
        stateService
    )

    private val user = User("user-1", "en", Type.FREE_USER, null)
    private val update: Update = mock()
    private val event = ButtonPressedEvent(update, user)

    @Test
    fun `skips and does not touch the task service when the message carries no document`() {
        whenever(update.message()).thenReturn(null)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        verify(convertationTaskService, never()).submitTask(any(), any(), any())
    }

    @Test
    fun `submits the task with the resolved file url and reports success when the file fits the size limit`() {
        val document = documentWithSize(50)
        val response = getFileResponse("remote-path.txt")
        val event = messageEvent(document)
        whenever(bot.execute(any<GetFile>())).thenReturn(response)
        whenever(bot.getFullFilePath(any<File>())).thenReturn("https://cdn/remote-path.txt")
        whenever(convertationTaskService.submitTask(any(), any(), any())).thenReturn(Either.Right(Unit))

        val result = runBlocking { handler.processEvent(event) }

        assertEquals(Either.Right(Unit), result)

        val sourceUrlCaptor = argumentCaptor<String>()
        val fileNameCaptor = argumentCaptor<String>()
        verify(convertationTaskService)
            .submitTask(eq(user), sourceUrlCaptor.capture(), fileNameCaptor.capture())
        assertEquals("https://cdn/remote-path.txt", sourceUrlCaptor.firstValue)
        assertEquals("book.txt", fileNameCaptor.firstValue)
    }

    @Test
    fun `rejects the document as too large before any task is submitted`() {
        val document = documentWithSize(101)

        val result = runBlocking { handler.processEvent(messageEvent(document)) }

        assertEquals(Either.Left(BookIsTooLargeError), result)
        verify(bot, never()).execute(any<GetFile>())
        verify(convertationTaskService, never()).submitTask(any(), any(), any())
    }

    @Test
    fun `maps a task queueing failure to the queueing error`() {
        val document = documentWithSize(50)
        val response = getFileResponse("remote-path.txt")
        val event = messageEvent(document)
        whenever(bot.execute(any<GetFile>())).thenReturn(response)
        whenever(bot.getFullFilePath(any<File>())).thenReturn("https://cdn/remote-path.txt")
        whenever(convertationTaskService.submitTask(any(), any(), any()))
            .thenReturn(Either.Left(FatalError("boom")))

        val result = runBlocking { handler.processEvent(event) }

        assertEquals(Either.Left(TaskQueueingError), result)
    }

    @Test
    fun `sends the requested message and resets the activity state when the document is processed`() {
        val document = documentWithSize(50)
        val response = getFileResponse("remote-path.txt")
        whenever(bot.execute(any<GetFile>())).thenReturn(response)
        whenever(bot.getFullFilePath(any<File>())).thenReturn("https://cdn/remote-path.txt")
        whenever(convertationTaskService.submitTask(any(), any(), any())).thenReturn(Either.Right(Unit))
        prepareInputState()

        val result = runBlocking { handler.handleEvent(messageEvent(document)) }

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)

        val messageCaptor = argumentCaptor<DomainMessage>()
        verify(messageSender).sendResponse(any<Update>(), eq(user), messageCaptor.capture(), any())
        assertTrue(messageCaptor.firstValue is FileConvertationRequestedMessage)
        verify(stateService).setCurrentState(eq("user-1"), isNull<String>())
    }

    @Test
    fun `sends the failure message and does not reset the activity state when the file is too large`() {
        val document = documentWithSize(101)
        prepareInputState()

        val result = runBlocking { handler.handleEvent(messageEvent(document)) }

        assertEquals(Either.Left(BookIsTooLargeError), result)

        val messageCaptor = argumentCaptor<DomainMessage>()
        verify(messageSender).sendResponse(any<Update>(), eq(user), messageCaptor.capture(), any())
        assertTrue(messageCaptor.firstValue is FileUploadFailedMessage)
        verify(stateService, never()).setCurrentState(any(), any())
    }

    private fun documentWithSize(size: Long): Document {
        val document: Document = mock()
        whenever(document.fileSize()).thenReturn(size)
        whenever(document.fileId()).thenReturn("file-id")
        whenever(document.fileName()).thenReturn("book.txt")
        return document
    }

    private fun getFileResponse(path: String): GetFileResponse {
        val file: File = mock()
        whenever(file.fileId()).thenReturn("file-id")
        whenever(file.filePath()).thenReturn(path)
        val response: GetFileResponse = mock()
        whenever(response.file()).thenReturn(file)
        return response
    }

    private fun prepareInputState() {
        whenever(stateService.fetchCurrentState("user-1")).thenReturn("RequestConvertationPromptButton")
        whenever(buttonService.instance("RequestConvertationPromptButton")).thenReturn(RequestConvertationPromptButton)
    }

    private fun messageEvent(document: Document): ButtonPressedEvent {
        val message: Message = mock()
        whenever(message.document()).thenReturn(document)
        whenever(update.message()).thenReturn(message)
        return event
    }
}
