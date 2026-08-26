package org.grakovne.sideload.kindle.telegram.handlers.screens

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendDocument
import com.pengrad.telegrambot.response.SendResponse
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.converter.FatalError
import org.grakovne.sideload.kindle.converter.FileNotSupported
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import org.grakovne.sideload.kindle.shelf.service.ShelfService
import org.grakovne.sideload.kindle.telegram.domain.error.UnknownError
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionErrorMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionFailedUnsupportedMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionSuccessAutomaticStkMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionSuccessEmptyOutputMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionSuccessMessage
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.user.common.OutputFormat
import org.grakovne.sideload.kindle.user.preferences.domain.UserPreferences
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookConversionFinishHandlerTest {

    private val bot: TelegramBot = mock()
    private val messageSender: MessageWithNavigationSender = mock()
    private val userService: UserService = mock()
    private val userPreferencesService: UserPreferencesService = mock()
    private val shelfService: ShelfService = mock()
    private val sut = BookConversionFinishHandler(bot, messageSender, userService, userPreferencesService, shelfService)

    private val user = User("user-1", "en", Type.FREE_USER, null)

    private fun preferences(automaticStk: Boolean) =
        UserPreferences(UUID.randomUUID(), "user-1", OutputFormat.EPUB, null, false, automaticStk)

    private fun capturedMessage(): Message {
        val captor = argumentCaptor<Message>()
        verify(messageSender).sendResponse(any<String>(), any(), captor.capture(), any())
        return captor.firstValue
    }

    @Test
    fun `reports processed when the conversion finished successfully`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user)
        whenever(userPreferencesService.fetchPreferences("user-1")).thenReturn(preferences(automaticStk = false))

        val result = runBlocking {
            sut.handleEvent(ConvertationFinishedEvent("user-1", ConvertationFinishedStatus.SUCCESS, "log", emptyList(), "env-1"))
        }

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
    }

    @Test
    fun `reports the error when the conversion failed`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user)

        val result = runBlocking {
            sut.handleEvent(ConvertationFinishedEvent("user-1", ConvertationFinishedStatus.FAILED, "log", emptyList(), "env-1"))
        }

        assertEquals(Either.Left(UnknownError), result)
    }

    @Test
    fun `sends the automatic stk message when automatic stk is enabled`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user)
        whenever(userPreferencesService.fetchPreferences("user-1")).thenReturn(preferences(automaticStk = true))

        sut.sendSuccessfulResponse(ConvertationFinishedEvent("user-1", ConvertationFinishedStatus.SUCCESS, "log", emptyList(), "env-1"))

        assertTrue(capturedMessage() is FileConvertarionSuccessAutomaticStkMessage)
    }

    @Test
    fun `sends the empty output message when there is no output`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user)
        whenever(userPreferencesService.fetchPreferences("user-1")).thenReturn(preferences(automaticStk = false))

        sut.sendSuccessfulResponse(ConvertationFinishedEvent("user-1", ConvertationFinishedStatus.SUCCESS, "log", emptyList(), "env-1"))

        assertTrue(capturedMessage() is FileConvertarionSuccessEmptyOutputMessage)
    }

    @Test
    fun `sends the success message with the shelf link when the output is present`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user)
        whenever(userPreferencesService.fetchPreferences("user-1")).thenReturn(preferences(automaticStk = false))
        whenever(shelfService.fetchShelfLink("user-1")).thenReturn("https://shelf.example.com/abcde")
        whenever(bot.execute(any<SendDocument>())).thenReturn(mock<SendResponse>())

        sut.sendSuccessfulResponse(ConvertationFinishedEvent("user-1", ConvertationFinishedStatus.SUCCESS, "log", listOf(File("book.azw3")), "env-1"))

        val message = capturedMessage() as FileConvertarionSuccessMessage
        assertEquals("https://shelf.example.com/abcde", message.bookShelfUrl)
    }

    @Test
    fun `sends the unsupported message when the failure reason is an unsupported file`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user)

        sut.sendFailureResponse(
            ConvertationFinishedEvent("user-1", ConvertationFinishedStatus.FAILED, "log", emptyList(), "env-1", FileNotSupported),
            UnknownError
        )

        assertTrue(capturedMessage() is FileConvertarionFailedUnsupportedMessage)
    }

    @Test
    fun `sends the generic error message with the log for any other failure reason`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user)

        sut.sendFailureResponse(
            ConvertationFinishedEvent("user-1", ConvertationFinishedStatus.FAILED, "boom", emptyList(), "env-1", FatalError("boom")),
            UnknownError
        )

        val message = capturedMessage() as FileConvertarionErrorMessage
        assertEquals("boom", message.details)
    }
}
