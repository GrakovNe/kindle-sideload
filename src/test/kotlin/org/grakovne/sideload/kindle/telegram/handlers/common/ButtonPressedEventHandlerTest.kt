package org.grakovne.sideload.kindle.telegram.handlers.common

import arrow.core.Either
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Update
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestProjectInfoButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestSettingButton
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class ButtonTestError : EventProcessingError

/**
 * Concrete [ButtonPressedEventHandler] so the abstract dispatch logic
 * (button matching, activity-state recording, result mapping) can be exercised.
 */
class ButtonTestHandler(
    buttonService: ButtonService,
    stateService: UserActivityStateService
) : ButtonPressedEventHandler<ButtonTestError>(buttonService, stateService) {

    var operated: List<Class<*>> = listOf(RequestSettingButton::class.java)
    var processResult: Either<ButtonTestError, Unit> = Either.Right(Unit)
    var processEventInvoked = false

    override fun getOperatingButtons(): List<Class<*>> = operated

    override fun processEvent(event: ButtonPressedEvent): Either<ButtonTestError, Unit> {
        processEventInvoked = true
        return processResult
    }
}

class ButtonPressedEventHandlerTest {

    private val buttonService: ButtonService = mock()
    private val stateService: UserActivityStateService = mock()
    private val update: Update = mock()
    private val callbackQuery: CallbackQuery = mock()
    private val handler = ButtonTestHandler(buttonService, stateService)
    private val event = ButtonPressedEvent(update, User("user-1", "en", Type.FREE_USER, null))

    private fun press(data: String, button: Button) {
        whenever(update.callbackQuery()).thenReturn(callbackQuery)
        whenever(callbackQuery.data()).thenReturn(data)
        whenever(buttonService.instance(data)).thenReturn(button)
    }

    @Test
    fun `processes the event and records the pressed button when it is operated`() {
        press("RequestSettingButton", RequestSettingButton)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
        assertEquals(true, handler.processEventInvoked)
        verify(stateService).setCurrentState(eq("user-1"), eq("RequestSettingButton"))
    }

    @Test
    fun `skips the event when the pressed button is not operated by this handler`() {
        press("RequestProjectInfoButton", RequestProjectInfoButton)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        assertEquals(false, handler.processEventInvoked)
        verify(stateService, never()).setCurrentState(any(), any())
    }

    @Test
    fun `propagates the error when processing the operated button fails`() {
        press("RequestSettingButton", RequestSettingButton)
        val error = ButtonTestError()
        handler.processResult = Either.Left(error)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Left(error), result)
        verify(stateService).setCurrentState(eq("user-1"), eq("RequestSettingButton"))
    }
}
