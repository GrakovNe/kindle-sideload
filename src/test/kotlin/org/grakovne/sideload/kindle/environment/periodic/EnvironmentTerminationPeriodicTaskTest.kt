package org.grakovne.sideload.kindle.environment.periodic

import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.environment.configuration.EnvironmentProperties
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.UserEnvironmentUnnecessaryEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvironmentTerminationPeriodicTaskTest {

    @TempDir
    lateinit var tempDir: File

    private val eventSender: EventSender = mock()
    private val environmentService: UserEnvironmentService = mock()
    private lateinit var properties: EnvironmentProperties
    private lateinit var sut: EnvironmentTerminationPeriodicTask

    @BeforeEach
    fun setUp() {
        properties = EnvironmentProperties().apply {
            temporaryFolder = File(tempDir, "environments").absolutePath
            outputFileExtensions = listOf("epub")
            ttlInSeconds = 0L
        }
        sut = EnvironmentTerminationPeriodicTask(eventSender, properties, environmentService)
    }

    @Test
    fun `sends a termination event for each outdated environment`() {
        val folder = File(tempDir, "environments").apply { mkdirs() }
        File(folder, "old-env-1").mkdirs()
        File(folder, "old-env-2").mkdirs()
        whenever(environmentService.provideTemporaryEnvironmentsFolder()).thenReturn(folder)

        sut.terminateOutdatedEnvironments()

        val events = argumentCaptor<UserEnvironmentUnnecessaryEvent>()
        verify(eventSender, times(2)).sendEvent(events.capture())
        assertEquals(listOf("old-env-1", "old-env-2"), events.allValues.mapNotNull { it.environmentId }.sorted())
    }

    @Test
    fun `skips plain files and only reports the environment directories`() {
        val folder = File(tempDir, "environments").apply { mkdirs() }
        File(folder, "old-env-1").mkdirs()
        File(folder, "some-file.txt").writeText("not a directory")
        whenever(environmentService.provideTemporaryEnvironmentsFolder()).thenReturn(folder)

        sut.terminateOutdatedEnvironments()

        val events = argumentCaptor<UserEnvironmentUnnecessaryEvent>()
        verify(eventSender, times(1)).sendEvent(events.capture())
        assertEquals(listOf("old-env-1"), events.allValues.mapNotNull { it.environmentId })
        assertTrue(File(folder, "some-file.txt").exists())
    }

    @Test
    fun `sends no events when the folder is empty`() {
        val folder = File(tempDir, "environments").apply { mkdirs() }
        whenever(environmentService.provideTemporaryEnvironmentsFolder()).thenReturn(folder)

        sut.terminateOutdatedEnvironments()

        verify(eventSender, never()).sendEvent(any<UserEnvironmentUnnecessaryEvent>())
    }
}
