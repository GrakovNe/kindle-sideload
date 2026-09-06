package org.grakovne.sideload.kindle.environment

import arrow.core.Either
import org.grakovne.sideload.kindle.common.ZipArchiveService
import org.grakovne.sideload.kindle.environment.configuration.EnvironmentProperties
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.grakovne.sideload.kindle.user.configuration.domain.ConfigurationNotFoundError
import org.grakovne.sideload.kindle.user.configuration.domain.UnableUpdateConfigurationError
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserEnvironmentServiceTest {

    @TempDir
    lateinit var tempDir: File

    private val configurationService: UserConverterConfigurationService = mock()
    private lateinit var properties: EnvironmentProperties
    private lateinit var sut: UserEnvironmentService

    @BeforeEach
    fun setUp() {
        properties = EnvironmentProperties().apply {
            temporaryFolder = File(tempDir, "environments").absolutePath
            outputFileExtensions = listOf("epub", "azw3")
            ttlInSeconds = 3600L
        }
        sut = UserEnvironmentService(configurationService, properties, ZipArchiveService())
    }

    @Test
    fun `provides only the output files of the environment`() {
        val environment = File(sut.provideTemporaryEnvironmentsFolder(), "env-1").apply {
            mkdirs()
            File(this, "book.epub").writeText("epub")
            File(this, "notes.txt").writeText("verbose")
            File(this, "nested").mkdirs()
        }

        val files = sut.provideEnvironmentFiles("env-1")

        assertEquals(listOf("book.epub"), files.map { it.name })
    }

    @Test
    fun `provides an empty list for an empty environment`() {
        File(sut.provideTemporaryEnvironmentsFolder(), "env-1").mkdirs()

        assertTrue(sut.provideEnvironmentFiles("env-1").isEmpty())
    }

    @Test
    fun `deploys the user configuration into a fresh environment folder`() {
        val configuration = configurationZip()
        whenever(configurationService.fetchConverterConfiguration("user-1")).thenReturn(Either.Right(configuration))

        val result = sut.deployEnvironment("user-1")

        assertTrue(result.isRight())
        val environment = result.getOrNull()!!
        assertEquals(File(tempDir, "environments"), environment.parentFile)
        assertTrue(environment.isDirectory)
        assertTrue(File(environment, "configuration.toml").readText() == "key = value")
        verify(configurationService).fetchConverterConfiguration("user-1")
    }

    @Test
    fun `deploys an empty environment when the user has no configuration`() {
        whenever(configurationService.fetchConverterConfiguration("user-1"))
            .thenReturn(Either.Left(ConfigurationNotFoundError))

        val result = sut.deployEnvironment("user-1")

        assertTrue(result.isRight())
        assertTrue(result.getOrNull()?.listFiles()?.isEmpty() == true)
    }

    @Test
    fun `reports an unable to deploy error for any other configuration error`() {
        whenever(configurationService.fetchConverterConfiguration("user-1"))
            .thenReturn(Either.Left(UnableUpdateConfigurationError))

        val result = sut.deployEnvironment("user-1")

        assertEquals(Either.Left(UnableDeployError), result)
    }

    @Test
    fun `terminates the environment folder`() {
        val environment = File(sut.provideTemporaryEnvironmentsFolder(), "env-1").apply { mkdirs() }

        val result = sut.terminateEnvironment("env-1")

        assertEquals(Either.Right(Unit), result)
        assertTrue(!environment.exists())
    }

    @Test
    fun `terminating an absent environment is a safe no-op`() {
        val result = sut.terminateEnvironment("missing-env")

        assertEquals(Either.Right(Unit), result)
        assertTrue(!File(sut.provideTemporaryEnvironmentsFolder(), "missing-env").exists())
    }

    private fun configurationZip(): File {
        val file = File(tempDir, "configuration.zip")
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.putNextEntry(ZipEntry("configuration.toml"))
            zip.write("key = value".toByteArray())
            zip.closeEntry()
        }
        return file
    }
}
