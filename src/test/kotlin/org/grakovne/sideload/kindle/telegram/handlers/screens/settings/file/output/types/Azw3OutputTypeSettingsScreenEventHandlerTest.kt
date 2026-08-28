package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.file.output.types

import arrow.core.Either
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Update
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.file.output.Azw3ModeButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.file.output.EpubOutputButton
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.common.OutputFormat
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
import org.grakovne.sideload.kindle.common.navigation.domain.Message as DomainMessage

class Azw3OutputTypeSettingsScreenEventHandlerTest {

    private val preferencesService: UserPreferencesService = mock()
    private val messageSender: MessageWithNavigationSender = mock()
    private val buttonService: ButtonService = mock()
    private val stateService: UserActivityStateService = mock()

    private val handler = Azw3OutputTypeSettingsScreenEventHandler(
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
        whenever(buttonService.instance(data)).thenReturn(Azw3ModeButton)
    }

    @Test
    fun `sets the azw3 output format for the user and reports success`() {
        press("Azw3ModeButton")

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
        verify(preferencesService).updateOutputFormat("user-1", OutputFormat.AZW3)
        verify(stateService).setCurrentState(eq("user-1"), eq("Azw3ModeButton"))

        val messageCaptor = argumentCaptor<DomainMessage>()
        verify(messageSender).sendResponse(any<Update>(), eq(user), messageCaptor.capture(), any())
        assertTrue(messageCaptor.firstValue is SetOutputTypeMessage)
    }

    @Test
    fun `skips the event when the pressed button is not the azw3 one`() {
        whenever(update.callbackQuery()).thenReturn(callbackQuery)
        whenever(callbackQuery.data()).thenReturn("EpubOutputButton")
        whenever(buttonService.instance("EpubOutputButton")).thenReturn(EpubOutputButton)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        verify(preferencesService, never()).updateOutputFormat(any(), any())
        verify(messageSender, never()).sendResponse(any<Update>(), any(), any(), any())
    }
}
