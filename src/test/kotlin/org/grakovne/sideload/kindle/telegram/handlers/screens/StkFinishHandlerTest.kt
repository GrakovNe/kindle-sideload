package org.grakovne.sideload.kindle.telegram.handlers.screens

import arrow.core.Either
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.internal.StkFinishedEvent
import org.grakovne.sideload.kindle.events.internal.StkFinishedStatus
import org.grakovne.sideload.kindle.telegram.domain.error.UnknownError
import org.grakovne.sideload.kindle.telegram.navigation.StkFailedMessage
import org.grakovne.sideload.kindle.telegram.navigation.StkSuccessAzwMessage
import org.grakovne.sideload.kindle.telegram.navigation.StkSuccessMessage
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
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StkFinishHandlerTest {

    private val messageSender: MessageWithNavigationSender = mock()
    private val userService: UserService = mock()
    private val userPreferencesService: UserPreferencesService = mock()
    private val sut = StkFinishHandler(messageSender, userService, userPreferencesService)

    private fun user(id: String = "user-1") = User(id, "en", Type.FREE_USER, null)

    private fun preferences(format: OutputFormat) =
        UserPreferences(UUID.randomUUID(), "user-1", format, null, false, false)

    private fun capturedMessage(): Message {
        val captor = argumentCaptor<Message>()
        verify(messageSender).sendResponse(any<String>(), any(), captor.capture(), any())
        return captor.firstValue
    }

    @Test
    fun `reports processed when stk finished successfully`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user())
        whenever(userPreferencesService.fetchPreferences("user-1")).thenReturn(preferences(OutputFormat.EPUB))

        val result = runBlocking { sut.handleEvent(StkFinishedEvent("user-1", StkFinishedStatus.SUCCESS)) }

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
    }

    @Test
    fun `reports the error when stk failed`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user())

        val result = runBlocking { sut.handleEvent(StkFinishedEvent("user-1", StkFinishedStatus.FAILED)) }

        assertEquals(Either.Left(UnknownError), result)
    }

    @Test
    fun `sends the azw3 success message when the output format is azw3`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user())
        whenever(userPreferencesService.fetchPreferences("user-1")).thenReturn(preferences(OutputFormat.AZW3))

        sut.sendSuccessfulResponse(StkFinishedEvent("user-1", StkFinishedStatus.SUCCESS))

        assertTrue(capturedMessage() is StkSuccessAzwMessage)
    }

    @Test
    fun `sends the generic success message for the other output formats`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user())
        whenever(userPreferencesService.fetchPreferences("user-1")).thenReturn(preferences(OutputFormat.EPUB))

        sut.sendSuccessfulResponse(StkFinishedEvent("user-1", StkFinishedStatus.SUCCESS))

        assertTrue(capturedMessage() is StkSuccessMessage)
    }

    @Test
    fun `sends the failure message when stk failed`() {
        whenever(userService.fetchUser("user-1")).thenReturn(user())

        sut.sendFailureResponse(StkFinishedEvent("user-1", StkFinishedStatus.FAILED), UnknownError)

        assertTrue(capturedMessage() is StkFailedMessage)
    }
}
