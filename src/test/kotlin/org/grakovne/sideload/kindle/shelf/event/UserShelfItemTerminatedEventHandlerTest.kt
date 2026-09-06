package org.grakovne.sideload.kindle.shelf.event

import arrow.core.Either
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.internal.UserEnvironmentUnnecessaryEvent
import org.grakovne.sideload.kindle.shelf.common.ShelfItemError
import org.grakovne.sideload.kindle.shelf.common.UnableTerminateItemError
import org.grakovne.sideload.kindle.shelf.service.ShelfItemService
import org.junit.jupiter.api.BeforeEach
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

class UserShelfItemTerminatedEventHandlerTest {

    private val shelfItemService = mock<ShelfItemService>()

    private lateinit var sut: UserShelfItemTerminatedEventHandler

    @BeforeEach
    fun setUp() {
        sut = UserShelfItemTerminatedEventHandler(shelfItemService)
    }

    @Test
    fun `terminates the shelf item of the unnecessary environment`() = runBlocking {
        whenever(shelfItemService.terminateItem(eq("env-1"))).thenReturn(Either.Right(Unit))

        val result = sut.handleEvent(UserEnvironmentUnnecessaryEvent(environmentId = "env-1"))

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
        val captor = argumentCaptor<String>()
        verify(shelfItemService).terminateItem(captor.capture())
        assertEquals("env-1", captor.firstValue)
    }

    @Test
    fun `reports the terminate error when there is no shelf item for the environment`() = runBlocking {
        whenever(shelfItemService.terminateItem(eq("env-1")))
            .thenReturn(Either.Left(ShelfItemError.ITEM_NOT_EXISTS))

        val result = sut.handleEvent(UserEnvironmentUnnecessaryEvent(environmentId = "env-1"))

        assertTrue(result.isLeft())
        assertEquals(UnableTerminateItemError, result.swap().getOrNull())
    }

    @Test
    fun `skips the event when the environment id is missing`() = runBlocking {
        val result = sut.handleEvent(UserEnvironmentUnnecessaryEvent(environmentId = null))

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        verify(shelfItemService, never()).terminateItem(any())
    }
}
