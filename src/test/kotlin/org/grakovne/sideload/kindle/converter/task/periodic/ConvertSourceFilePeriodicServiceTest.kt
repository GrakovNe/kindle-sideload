package org.grakovne.sideload.kindle.converter.task.periodic

import arrow.core.Either
import org.grakovne.sideload.kindle.common.FileDownloadService
import org.grakovne.sideload.kindle.converter.ConversionResult
import org.grakovne.sideload.kindle.converter.ConvertationError
import org.grakovne.sideload.kindle.converter.ConverterService
import org.grakovne.sideload.kindle.converter.FatalError
import org.grakovne.sideload.kindle.converter.UnableConvertFile
import org.grakovne.sideload.kindle.converter.UnableFetchFile
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConvertSourceFilePeriodicServiceTest {

    @TempDir
    lateinit var tempDir: File

    private val downloadService: FileDownloadService = mock()
    private val converterService: ConverterService = mock()
    private val taskService: ConvertationTaskService = mock()
    private val eventSender: EventSender = mock()

    private lateinit var sut: ConvertSourceFilePeriodicService
    private lateinit var downloaded: File

    @BeforeEach
    fun setUp() {
        sut = ConvertSourceFilePeriodicService(downloadService, converterService, taskService, eventSender)
        downloaded = File(tempDir, "book.fb2").apply { writeText("book content") }
    }

    @Test
    fun `converts the downloaded file and marks the task as successful`() {
        whenever(taskService.fetchTasksForProcessing()).thenReturn(listOf(task()))
        runBlocking { whenever(downloadService.download(any<String>(), any<String>())).thenReturn(downloaded) }
        whenever(converterService.convertAndCollect(any<String>(), any<File>()))
            .thenReturn(Either.Right(ConversionResult("all good", "env-1", listOf(downloaded))))

        sut.convertSourceFiles()

        val events = argumentCaptor<ConvertationFinishedEvent>()
        verify(eventSender).sendEvent(events.capture())
        val event = events.firstValue
        assertEquals(ConvertationFinishedStatus.SUCCESS, event.status)
        assertEquals("all good", event.log)
        assertEquals(listOf(downloaded), event.output)
        assertEquals("env-1", event.environmentId)
        assertEquals(null, event.failureReason)

        val tasks = argumentCaptor<ConvertationTask>()
        verify(taskService).updateTask(tasks.capture())
        assertEquals(ConvertationTaskStatus.SUCCESS, tasks.firstValue.status)
    }

    @Test
    fun `marks the task as failed and reports the conversion error`() {
        whenever(taskService.fetchTasksForProcessing()).thenReturn(listOf(task()))
        runBlocking { whenever(downloadService.download(any<String>(), any<String>())).thenReturn(downloaded) }
        whenever(converterService.convertAndCollect(any<String>(), any<File>()))
            .thenReturn(Either.Left(UnableConvertFile("boom", "env-1")))

        sut.convertSourceFiles()

        val events = argumentCaptor<ConvertationFinishedEvent>()
        verify(eventSender).sendEvent(events.capture())
        val event = events.firstValue
        assertEquals(ConvertationFinishedStatus.FAILED, event.status)
        assertEquals("boom", event.log)
        assertEquals(UnableConvertFile("boom", "env-1"), event.failureReason)

        val tasks = argumentCaptor<ConvertationTask>()
        verify(taskService).updateTask(tasks.capture())
        assertEquals(ConvertationTaskStatus.FAILED, tasks.firstValue.status)
        assertTrue(tasks.firstValue.failReason.orEmpty().contains("UnableConvertFile"))
        assertTrue(tasks.firstValue.failReason.orEmpty().contains("boom"))
    }

    @Test
    fun `reports a fatal error with a dedicated message when the conversion throws`() {
        whenever(taskService.fetchTasksForProcessing()).thenReturn(listOf(task()))
        runBlocking { whenever(downloadService.download(any<String>(), any<String>())).thenReturn(downloaded) }
        whenever(converterService.convertAndCollect(any<String>(), any<File>()))
            .thenThrow(IllegalStateException("unexpected"))

        sut.convertSourceFiles()

        val events = argumentCaptor<ConvertationFinishedEvent>()
        verify(eventSender).sendEvent(events.capture())
        val event = events.firstValue
        assertEquals(ConvertationFinishedStatus.FAILED, event.status)
        assertEquals("Fatal Error occurred on file processing. ", event.log)
        val reason = event.failureReason
        assertTrue(reason is FatalError)
        assertTrue(reason.details.contains("unexpected"))
    }

    @Test
    fun `reports an unable to fetch error when the download returns nothing`() {
        whenever(taskService.fetchTasksForProcessing()).thenReturn(listOf(task()))
        runBlocking { whenever(downloadService.download(any<String>(), any<String>())).thenReturn(null) }

        sut.convertSourceFiles()

        val events = argumentCaptor<ConvertationFinishedEvent>()
        verify(eventSender).sendEvent(events.capture())
        assertEquals(ConvertationFinishedStatus.FAILED, events.firstValue.status)
        assertEquals("", events.firstValue.log)
        assertEquals(UnableFetchFile, events.firstValue.failureReason)

        val tasks = argumentCaptor<ConvertationTask>()
        verify(taskService).updateTask(tasks.capture())
        assertEquals(ConvertationTaskStatus.FAILED, tasks.firstValue.status)
    }

    @Test
    fun `does not send events when there is nothing to process`() {
        whenever(taskService.fetchTasksForProcessing()).thenReturn(emptyList())

        sut.convertSourceFiles()

        verify(eventSender, org.mockito.kotlin.never()).sendEvent(any<ConvertationFinishedEvent>())
        verify(taskService, org.mockito.kotlin.never()).updateTask(any<ConvertationTask>())
    }

    private fun task() = ConvertationTask(
        id = UUID.randomUUID(),
        userId = "user-1",
        sourceFileUrl = "https://example.com/book.fb2",
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        failReason = null,
        status = ConvertationTaskStatus.ACTIVE,
        fileName = "book.fb2"
    )
}
