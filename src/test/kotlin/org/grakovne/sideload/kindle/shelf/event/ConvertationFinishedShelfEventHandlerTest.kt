package org.grakovne.sideload.kindle.shelf.event

import arrow.core.Either
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import org.grakovne.sideload.kindle.shelf.common.ShelfItemError
import org.grakovne.sideload.kindle.shelf.common.UnableAttachItemError
import org.grakovne.sideload.kindle.shelf.domain.ShelfReference
import org.grakovne.sideload.kindle.shelf.service.ShelfItemService
import org.grakovne.sideload.kindle.shelf.service.ShelfService
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

class ConvertationFinishedShelfEventHandlerTest {

    private val shelfService = mock<ShelfService>()
    private val shelfItemService = mock<ShelfItemService>()

    private lateinit var sut: ConvertationFinishedShelfEventHandler

    @BeforeEach
    fun setUp() {
        sut = ConvertationFinishedShelfEventHandler(shelfService, shelfItemService)
    }

    @Test
    fun `attaches the converted book to the user shelf`() = runBlocking {
        val shelf = shelfReference("user-1")
        whenever(shelfService.fetchOrCreateShelf(eq("user-1"))).thenReturn(shelf)
        whenever(shelfItemService.attachToShelf(any(), anyOrNull())).thenReturn(Either.Right(Unit))

        val result = sut.handleEvent(
            event(ConvertationFinishedStatus.SUCCESS, environmentId = "env-1")
        )

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
        val shelfCaptor = argumentCaptor<UUID>()
        val envCaptor = argumentCaptor<String>()
        verify(shelfItemService).attachToShelf(shelfCaptor.capture(), envCaptor.capture())
        assertEquals(shelf.id, shelfCaptor.firstValue)
        assertEquals("env-1", envCaptor.firstValue)
    }

    @Test
    fun `reports the attach error when the item is already on the shelf`() = runBlocking {
        val shelf = shelfReference("user-1")
        whenever(shelfService.fetchOrCreateShelf(eq("user-1"))).thenReturn(shelf)
        whenever(shelfItemService.attachToShelf(any(), anyOrNull()))
            .thenReturn(Either.Left(ShelfItemError.ITEM_ALREADY_EXISTS))

        val result = sut.handleEvent(
            event(ConvertationFinishedStatus.SUCCESS, environmentId = "env-1")
        )

        assertTrue(result.isLeft())
        assertEquals(UnableAttachItemError, result.swap().getOrNull())
    }

    @Test
    fun `reports the attach error when the environment id is missing`() = runBlocking {
        val result = sut.handleEvent(
            event(ConvertationFinishedStatus.SUCCESS, environmentId = null)
        )

        assertTrue(result.isLeft())
        assertEquals(UnableAttachItemError, result.swap().getOrNull())
        verify(shelfService, never()).fetchOrCreateShelf(any())
    }

    @Test
    fun `skips the event when the conversion did not succeed`() = runBlocking {
        val result = sut.handleEvent(
            event(ConvertationFinishedStatus.FAILED, environmentId = "env-1")
        )

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        verify(shelfService, never()).fetchOrCreateShelf(any())
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

    private fun shelfReference(userId: String) = ShelfReference(
        id = UUID.randomUUID(),
        shortId = "abcde",
        userId = userId
    )
}
