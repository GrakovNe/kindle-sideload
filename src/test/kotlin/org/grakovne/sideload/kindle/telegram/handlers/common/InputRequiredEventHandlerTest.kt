package org.grakovne.sideload.kindle.telegram.handlers.common

import arrow.core.Either
import com.pengrad.telegrambot.model.Update
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestSettingButton
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class InputTestError : EventProcessingError

/**
 * Concrete [InputRequiredEventHandler] so the abstract dispatch logic
 * (activity-state lookup, required-button matching, state reset) can be exercised.
 */
class InputTestHandler(
    stateService: UserActivityStateService,
    buttonService: ButtonService
) : InputRequiredEventHandler<InputTestError>(stateService, buttonService) {

    var requiredButtons: List<Button> = listOf(RequestSettingButton)
    var processResult: Either<InputTestError, Unit> = Either.Right(Unit)
    var processEventInvoked = false

    override fun getRequiredButton(): List<Button> = requiredButtons

    override suspend fun processEvent(event: ButtonPressedEvent): Either<InputTestError, Unit> {
        processEventInvoked = true
        return processResult
    }
}

class InputRequiredEventHandlerTest {

    private val buttonService: ButtonService = mock()
    private val stateService: UserActivityStateService = mock()
    private val update: Update = mock()
    private val handler = InputTestHandler(stateService, buttonService)
    private val event = ButtonPressedEvent(update, User("user-1", "en", Type.FREE_USER, null))

    @Test
    fun `skips when the user has no current activity state`() {
        whenever(stateService.fetchCurrentState("user-1")).thenReturn(null)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        assertEquals(false, handler.processEventInvoked)
        verify(stateService, never()).setCurrentState(any(), any())
    }

    @Test
    fun `skips when the recorded state does not resolve to a button`() {
        whenever(stateService.fetchCurrentState("user-1")).thenReturn("SomeButton")
        whenever(buttonService.instance("SomeButton")).thenReturn(null)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        assertEquals(false, handler.processEventInvoked)
        verify(stateService, never()).setCurrentState(any(), any())
    }

    @Test
    fun `skips when the resolved button is not the one this handler requires`() {
        whenever(stateService.fetchCurrentState("user-1")).thenReturn("RequestSettingButton")
        whenever(buttonService.instance("RequestSettingButton")).thenReturn(RequestSettingButton)
        handler.requiredButtons = emptyList()

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        assertEquals(false, handler.processEventInvoked)
        verify(stateService, never()).setCurrentState(any(), any())
    }

    @Test
    fun `processes the event and resets the state when the required button is pending`() {
        whenever(stateService.fetchCurrentState("user-1")).thenReturn("RequestSettingButton")
        whenever(buttonService.instance("RequestSettingButton")).thenReturn(RequestSettingButton)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
        assertEquals(true, handler.processEventInvoked)
        verify(stateService).setCurrentState(eq("user-1"), isNull())
    }

    @Test
    fun `propagates the error and keeps the state when processing fails`() {
        whenever(stateService.fetchCurrentState("user-1")).thenReturn("RequestSettingButton")
        whenever(buttonService.instance("RequestSettingButton")).thenReturn(RequestSettingButton)
        val error = InputTestError()
        handler.processResult = Either.Left(error)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Left(error), result)
        verify(stateService, never()).setCurrentState(any(), any())
    }
}
