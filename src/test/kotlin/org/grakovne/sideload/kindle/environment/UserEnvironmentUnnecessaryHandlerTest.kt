package org.grakovne.sideload.kindle.environment

import arrow.core.Either
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.internal.UserEnvironmentUnnecessaryEvent
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class UserEnvironmentUnnecessaryHandlerTest {

    private val environmentService: UserEnvironmentService = mock()
    private val sut = UserEnvironmentUnnecessaryHandler(environmentService)

    @Test
    fun `accepts only the environment unnecessary event type`() {
        assertEquals(listOf(EnvironmentUnnecessary), sut.acceptableEvents())
    }

    @Test
    fun `terminates the environment reported by the event`() = runBlocking {
        whenever(environmentService.terminateEnvironment("env-1")).thenReturn(Either.Right(Unit))

        val result = sut.handleEvent(UserEnvironmentUnnecessaryEvent(environmentId = "env-1"))

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
        verify(environmentService).terminateEnvironment("env-1")
    }

    @Test
    fun `skips the processing when the environment id is missing`() = runBlocking {
        val result = sut.handleEvent(UserEnvironmentUnnecessaryEvent(environmentId = null))

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        verify(environmentService, never()).terminateEnvironment(any())
    }

    @Test
    fun `propagates the termination error`() = runBlocking {
        whenever(environmentService.terminateEnvironment("env-1")).thenReturn(Either.Left(UnableTerminateError))

        val result = sut.handleEvent(UserEnvironmentUnnecessaryEvent(environmentId = "env-1"))

        assertEquals(Either.Left(UnableTerminateError), result)
    }
}
