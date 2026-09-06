package org.grakovne.sideload.kindle.stk.event

import arrow.core.Either
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.converter.StkLimitExhausted
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import org.grakovne.sideload.kindle.stk.email.task.domain.InternalError
import org.grakovne.sideload.kindle.stk.email.task.service.TransferEmailTaskService
import org.grakovne.sideload.kindle.user.common.OutputFormat
import org.grakovne.sideload.kindle.user.preferences.domain.UserPreferences
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConvertationFinishedAutoStkEventHandlerTest {

    private val userPreferencesService = mock<UserPreferencesService>()
    private val transferEmailTaskService = mock<TransferEmailTaskService>()

    private lateinit var sut: ConvertationFinishedAutoStkEventHandler

    @BeforeEach
    fun setUp() {
        sut = ConvertationFinishedAutoStkEventHandler(userPreferencesService, transferEmailTaskService)
    }

    @Test
    fun `skips the event when the conversion failed`() = runBlocking {
        val result = sut.handleEvent(
            event(status = ConvertationFinishedStatus.FAILED, environmentId = "env-1")
        )

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        verify(userPreferencesService, never()).fetchPreferences(any())
    }

    @Test
    fun `submits the stk task when auto stk is enabled and the environment is known`() = runBlocking {
        whenever(userPreferencesService.fetchPreferences(eq("user-1")))
            .thenReturn(preferences(automaticStk = true))
        whenever(transferEmailTaskService.submitTask(any(), anyOrNull()))
            .thenReturn(Either.Right(Unit))

        val result = sut.handleEvent(event(status = ConvertationFinishedStatus.SUCCESS, environmentId = "env-1"))

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
        val userCaptor = argumentCaptor<String>()
        val envCaptor = argumentCaptor<String>()
        verify(transferEmailTaskService).submitTask(userCaptor.capture(), envCaptor.capture())
        assertEquals("user-1", userCaptor.firstValue)
        assertEquals("env-1", envCaptor.firstValue)
    }

    @Test
    fun `skips the event when auto stk is enabled but the environment id is missing`() = runBlocking {
        whenever(userPreferencesService.fetchPreferences(eq("user-1")))
            .thenReturn(preferences(automaticStk = true))

        val result = sut.handleEvent(event(status = ConvertationFinishedStatus.SUCCESS, environmentId = null))

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        verify(transferEmailTaskService, never()).submitTask(any(), anyOrNull())
    }

    @Test
    fun `skips the event when auto stk is disabled`() = runBlocking {
        whenever(userPreferencesService.fetchPreferences(eq("user-1")))
            .thenReturn(preferences(automaticStk = false))

        val result = sut.handleEvent(event(status = ConvertationFinishedStatus.SUCCESS, environmentId = "env-1"))

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        verify(transferEmailTaskService, never()).submitTask(any(), anyOrNull())
    }

    @Test
    fun `reports the internal error when the stk task cannot be submitted`() = runBlocking {
        whenever(userPreferencesService.fetchPreferences(eq("user-1")))
            .thenReturn(preferences(automaticStk = true))
        whenever(transferEmailTaskService.submitTask(eq("user-1"), eq("env-1")))
            .thenReturn(Either.Left(StkLimitExhausted))

        val result = sut.handleEvent(event(status = ConvertationFinishedStatus.SUCCESS, environmentId = "env-1"))

        assertTrue(result.isLeft())
        assertEquals(InternalError, result.swap().getOrNull())
    }

    private fun event(
        status: ConvertationFinishedStatus,
        environmentId: String?
    ) = ConvertationFinishedEvent(
        userId = "user-1",
        status = status,
        log = "log",
        output = emptyList(),
        environmentId = environmentId
    )

    private fun preferences(automaticStk: Boolean) = UserPreferences(
        id = UUID.randomUUID(),
        userId = "user-1",
        outputFormat = OutputFormat.EPUB,
        email = null,
        debugMode = false,
        automaticStk = automaticStk
    )
}
