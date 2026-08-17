package org.grakovne.sideload.kindle.converter

import arrow.core.Either
import org.grakovne.sideload.kindle.environment.UnableDeployError
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.environment.configuration.EnvironmentProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EpubBypassConverterServiceTest {

    @TempDir
    lateinit var tempDir: File

    private val userEnvironmentService: UserEnvironmentService = org.mockito.kotlin.mock()
    private val environmentProperties = EnvironmentProperties().apply {
        outputFileExtensions = listOf("epub", "azw3")
    }
    private val sut = EpubBypassConverterService(userEnvironmentService, environmentProperties)

    private fun environmentFor(userId: String): File {
        val environment = File(tempDir, "env-$userId").apply { mkdirs() }
        whenever(userEnvironmentService.deployEnvironment(userId)).thenReturn(Either.Right(environment))
        return environment
    }

    @Test
    fun `deploys the epub into the environment and reports it as output`() {
        environmentFor("user-1")
        val book = File(tempDir, "book.epub").apply { writeText("epub content") }

        val result = sut.convertAndCollect("user-1", book)

        assertTrue(result.isRight())
        val conversion = result.orNull()
        assertEquals("Bypass conversion completed: book.epub", conversion?.log)
        assertEquals("env-user-1", conversion?.environmentId)
        assertEquals(listOf("book.epub"), conversion?.output?.map { it.name })
        // the book was actually copied into the environment
        assertEquals("epub content", File(File(tempDir, "env-user-1"), "book.epub").readText())
    }

    @Test
    fun `files with a non output extension are dropped from the output`() {
        environmentFor("user-2")
        val book = File(tempDir, "book.txt").apply { writeText("not an epub") }

        val result = sut.convertAndCollect("user-2", book)

        val conversion = result.orNull()
        assertEquals("Bypass conversion completed: book.txt", conversion?.log)
        assertEquals(emptyList<File>(), conversion?.output)
    }

    @Test
    fun `reports an unable to deploy error when the environment deployment fails`() {
        whenever(userEnvironmentService.deployEnvironment("user-3"))
            .thenReturn(Either.Left(UnableDeployError))

        val book = File(tempDir, "book.epub").apply { writeText("epub") }

        val result = sut.convertAndCollect("user-3", book)

        assertTrue(result.isLeft())
        assertEquals(UnableDeployEnvironment, result.fold(ifLeft = { it }, ifRight = { throw AssertionError() }))
    }
}
