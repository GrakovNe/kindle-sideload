package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.stk.auto

import arrow.core.Either
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Update
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.common.navigation.domain.Message as DomainMessage
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestProjectInfoButton
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
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

class DisableAutoStkScreenEventHandlerTest {

    private val preferencesService: UserPreferencesService = mock()
    private val messageSender: MessageWithNavigationSender = mock()
    private val buttonService: ButtonService = mock()
    private val stateService: UserActivityStateService = mock()

    private val handler = DisableAutoStkScreenEventHandler(
        preferencesService,
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
        whenever(buttonService.instance(data)).thenReturn(DisableAutoStkButton)
    }

    @Test
    fun `disables the automatic stk for the user and reports success`() {
        press("DisableAutoStkButton")

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
        verify(preferencesService).updateAutomaticStk("user-1", false)
        verify(stateService).setCurrentState(eq("user-1"), eq("DisableAutoStkButton"))

        val messageCaptor = argumentCaptor<DomainMessage>()
        verify(messageSender).sendResponse(any<Update>(), eq(user), messageCaptor.capture(), any())
        assertTrue(messageCaptor.firstValue is DisableAutoStkSettingsMessage)
    }

    @Test
    fun `skips the event when the pressed button is not the auto stk one`() {
        whenever(update.callbackQuery()).thenReturn(callbackQuery)
        whenever(callbackQuery.data()).thenReturn("RequestProjectInfoButton")
        whenever(buttonService.instance("RequestProjectInfoButton")).thenReturn(RequestProjectInfoButton)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        verify(preferencesService, never()).updateAutomaticStk(any(), any())
        verify(messageSender, never()).sendResponse(any<Update>(), any(), any(), any())
    }
}
