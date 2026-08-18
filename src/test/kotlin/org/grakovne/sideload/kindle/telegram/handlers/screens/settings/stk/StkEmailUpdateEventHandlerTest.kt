package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.stk

import arrow.core.Either
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.common.navigation.domain.Message as DomainMessage
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.configuration.domain.EmailNotValidError
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
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

class StkEmailUpdateEventHandlerTest {

    private val stateService: UserActivityStateService = mock()
    private val buttonService: ButtonService = mock()
    private val messageSender: MessageWithNavigationSender = mock()
    private val preferencesService: UserPreferencesService = mock()

    private val handler = StkEmailUpdateEventHandler(
        stateService,
        buttonService,
        messageSender,
        preferencesService
    )

    private val user = User("user-1", "en", Type.FREE_USER, null)
    private val update: Update = mock()
    private val event = ButtonPressedEvent(update, user)

    @Test
    fun `skips the event when the incoming message has no text`() {
        whenever(update.message()).thenReturn(null)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        verify(preferencesService, never()).updateEmail(any(), any())
    }

    @Test
    fun `updates the email and reports success when the state matches the prompt button`() {
        prepareState()
        val message = messageWithText("user@example.com")
        whenever(update.message()).thenReturn(message)
        whenever(preferencesService.updateEmail("user-1", "user@example.com")).thenReturn(Either.Right(Unit))

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
        verify(preferencesService).updateEmail("user-1", "user@example.com")
        verify(stateService).setCurrentState(eq("user-1"), isNull())
    }

    @Test
    fun `reports the failure, sends the failure message and keeps the activity state when the email is invalid`() {
        prepareState()
        val message = messageWithText("not-an-email")
        whenever(update.message()).thenReturn(message)
        whenever(preferencesService.updateEmail("user-1", "not-an-email")).thenReturn(Either.Left(EmailNotValidError))

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Left(EmailNotValidError), result)

        val messageCaptor = argumentCaptor<DomainMessage>()
        verify(messageSender).sendResponse(any<Update>(), eq(user), messageCaptor.capture(), any())
        assertTrue(messageCaptor.firstValue is UpdateEmailUpdateFailedMessage)
        verify(stateService, never()).setCurrentState(any(), any())
    }

    @Test
    fun `sends the updated message when the email is accepted`() {
        prepareState()
        val message = messageWithText("user@example.com")
        whenever(update.message()).thenReturn(message)
        whenever(preferencesService.updateEmail("user-1", "user@example.com")).thenReturn(Either.Right(Unit))

        runBlocking { handler.handleEvent(event) }

        val messageCaptor = argumentCaptor<DomainMessage>()
        verify(messageSender).sendResponse(any<Update>(), eq(user), messageCaptor.capture(), any())
        assertTrue(messageCaptor.firstValue is UpdateEmailUpdatedMessage)
    }

    private fun prepareState() {
        whenever(stateService.fetchCurrentState("user-1")).thenReturn("UpdateStkEmailPromptButton")
        whenever(buttonService.instance("UpdateStkEmailPromptButton")).thenReturn(UpdateStkEmailPromptButton)
    }

    private fun messageWithText(text: String): Message {
        val message: Message = mock()
        whenever(message.text()).thenReturn(text)
        return message
    }
}
