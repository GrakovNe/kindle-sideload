package org.grakovne.sideload.kindle.telegram.handlers.screens.convertation

import arrow.core.Either
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Update
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.converter.StkLimitExhausted
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.stk.email.task.domain.InternalError
import org.grakovne.sideload.kindle.stk.email.task.service.TransferEmailTaskService
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.navigation.StkFailedMessage
import org.grakovne.sideload.kindle.telegram.navigation.StkSubmittedMessage
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.grakovne.sideload.kindle.common.navigation.domain.Message as DomainMessage

class BookEmailSideloadRequestHandlerTest {

    private val transferEmailTaskService: TransferEmailTaskService = mock()
    private val userService: UserService = mock()
    private val messageSender: MessageWithNavigationSender = mock()
    private val buttonService: ButtonService = mock()
    private val stateService: UserActivityStateService = mock()

    private val handler = BookEmailSideloadRequestHandler(
        transferEmailTaskService,
        userService,
        messageSender,
        buttonService,
        stateService
    )

    private val user = User("user-1", "en", Type.FREE_USER, null)
    private val update: Update = mock()
    private val callbackQuery: CallbackQuery = mock()
    private val event = ButtonPressedEvent(update, user)

    private fun press(data: String) {
        whenever(update.callbackQuery()).thenReturn(callbackQuery)
        whenever(callbackQuery.data()).thenReturn(data)
        whenever(buttonService.instance(data)).thenReturn(SendConvertedToEmailButton(environmentId = data.substringAfterLast('#')))
    }

    @Test
    fun `submits the transfer task with the environment id parsed from the button payload`() {
        press("SendConvertedToEmailButton#env-42")
        whenever(transferEmailTaskService.submitTask("user-1", environmentId = "env-42")).thenReturn(Either.Right(Unit))
        whenever(userService.fetchUser("user-1")).thenReturn(user)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
        verify(transferEmailTaskService).submitTask("user-1", environmentId = "env-42")

        val messageCaptor = argumentCaptor<DomainMessage>()
        verify(messageSender).sendResponse(eq("user-1"), eq(user), messageCaptor.capture(), any())
        assertTrue(messageCaptor.firstValue is StkSubmittedMessage)
    }

    @Test
    fun `skips the event and sends nothing when the callback carries no data`() {
        // A null callback data resolves to no pressed button, so the handler
        // never matches; pin that the event is skipped without touching the
        // task service or the message sender.
        whenever(update.callbackQuery()).thenReturn(callbackQuery)
        whenever(callbackQuery.data()).thenReturn(null)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        verify(transferEmailTaskService, never()).submitTask(any(), any())
        verify(messageSender, never()).sendResponse(any<String>(), any(), any(), any())
    }

    @Test
    fun `maps a task submission failure to the internal error and sends the failed message`() {
        press("SendConvertedToEmailButton#env-42")
        whenever(transferEmailTaskService.submitTask("user-1", environmentId = "env-42"))
            .thenReturn(Either.Left(StkLimitExhausted))
        whenever(userService.fetchUser("user-1")).thenReturn(user)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Left(InternalError), result)

        val messageCaptor = argumentCaptor<DomainMessage>()
        verify(messageSender).sendResponse(eq("user-1"), eq(user), messageCaptor.capture(), any())
        assertTrue(messageCaptor.firstValue is StkFailedMessage)
    }

    @Test
    fun `sends the submitted message with the user's chat id when the task is accepted`() {
        press("SendConvertedToEmailButton#env-42")
        whenever(transferEmailTaskService.submitTask("user-1", environmentId = "env-42")).thenReturn(Either.Right(Unit))
        whenever(userService.fetchUser("user-1")).thenReturn(user)

        runBlocking { handler.handleEvent(event) }

        val messageCaptor = argumentCaptor<DomainMessage>()
        verify(messageSender).sendResponse(eq("user-1"), eq(user), messageCaptor.capture(), any())
        assertTrue(messageCaptor.firstValue is StkSubmittedMessage)
    }
}
