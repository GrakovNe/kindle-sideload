package org.grakovne.sideload.kindle.converter

import arrow.core.Either
import org.grakovne.sideload.kindle.common.CliRunner
import org.grakovne.sideload.kindle.common.validation.ValidationError
import org.grakovne.sideload.kindle.converter.binary.configuration.ConverterBinaryProperties
import org.grakovne.sideload.kindle.converter.binary.provider.ConverterBinaryProvider
import org.grakovne.sideload.kindle.converter.validation.ConvertationFileValidationError
import org.grakovne.sideload.kindle.converter.validation.ConvertationFileValidationService
import org.grakovne.sideload.kindle.environment.UnableDeployError
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.environment.configuration.EnvironmentProperties
import org.grakovne.sideload.kindle.user.common.OutputFormat
import org.grakovne.sideload.kindle.user.preferences.domain.UserPreferences
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Fb2ConverterServiceTest {

    @TempDir
    lateinit var tempDir: File

    private val cliRunner: CliRunner = mock()
    private val userEnvironmentService: UserEnvironmentService = mock()
    private val binaryProvider: ConverterBinaryProvider = mock()
    private val userPreferencesService: UserPreferencesService = mock()
    private val validationService: ConvertationFileValidationService = mock()

    private val binaryProperties = ConverterBinaryProperties().apply {
        shell = "/bin/bash"
        shellArgs = "-c"
        converterFileName = "fb2c"
        configurationExtensions = listOf("toml")
        converterParameters = "--verbose"
    }

    private val environmentProperties = EnvironmentProperties().apply {
        outputFileExtensions = listOf("epub", "azw3")
    }

    private val preferences = UserPreferences(
        id = UUID.randomUUID(),
        userId = "user-1",
        outputFormat = OutputFormat.AZW3,
        email = null,
        debugMode = false,
        automaticStk = false
    )

    private val sut = Fb2ConverterService(
        cliRunner = cliRunner,
        userEnvironmentService = userEnvironmentService,
        binaryProvider = binaryProvider,
        binaryProperties = binaryProperties,
        environmentProperties = environmentProperties,
        userPreferencesService = userPreferencesService,
        validationService = validationService
    )

    private lateinit var environment: File

    @BeforeEach
    fun setUp() {
        environment = File(tempDir, "fb2-env").apply { mkdirs() }
        // a converter binary that the production code marks executable
        File(environment, "fb2c").apply { writeText("fake fb2c") }
        whenever(userEnvironmentService.deployEnvironment("user-1")).thenReturn(Either.Right(environment))
        whenever(userPreferencesService.fetchPreferences("user-1")).thenReturn(preferences)
        whenever(binaryProvider.provideBinaryConverter()).thenReturn(File(environment, "fb2c"))
    }

    @Test
    fun `produces the output file reported by the converter`() {
        whenever(validationService.validate(any<File>())).thenReturn(Either.Right(Unit))
        whenever(cliRunner.runCli(any<String>(), any<String>(), any<String>(), any<File>()))
            .thenAnswer {
                File(environment, "book.azw3").writeText("converted azw3")
                Either.Right("conversion log")
            }

        val result = sut.convertAndCollect("user-1", book("book.fb2"))

        assertTrue(result.isRight())
        val conversion = result.getOrNull()
        assertEquals("conversion log", conversion?.log)
        assertEquals("fb2-env", conversion?.environmentId)
        assertEquals(listOf("book.azw3"), conversion?.output?.map { it.name })
    }

    @Test
    fun `drops verbose files when debug mode is off`() {
        whenever(validationService.validate(any<File>())).thenReturn(Either.Right(Unit))
        whenever(cliRunner.runCli(any<String>(), any<String>(), any<String>(), any<File>()))
            .thenAnswer {
                File(environment, "book.azw3").writeText("azw3")
                File(environment, "book.azw3.tmp").writeText("verbose temp")
                Either.Right("log")
            }

        val result = sut.convertAndCollect("user-1", book("book.fb2"))

        assertEquals(listOf("book.azw3"), result.getOrNull()?.output?.map { it.name })
    }

    @Test
    fun `keeps verbose files when debug mode is on`() {
        whenever(userPreferencesService.fetchPreferences("user-1"))
            .thenReturn(preferences.copy(debugMode = true))
        whenever(validationService.validate(any<File>())).thenReturn(Either.Right(Unit))
        whenever(cliRunner.runCli(any<String>(), any<String>(), any<String>(), any<File>()))
            .thenAnswer {
                File(environment, "book.azw3").writeText("azw3")
                File(environment, "book.azw3.tmp").writeText("verbose temp")
                Either.Right("log")
            }

        val result = sut.convertAndCollect("user-1", book("book.fb2"))

        assertEquals(setOf("book.azw3", "book.azw3.tmp"), result.getOrNull()?.output?.map { it.name }?.toSet())
    }

    @Test
    fun `reports file not supported when validation fails`() {
        whenever(validationService.validate(any<File>())).thenReturn(
            Either.Left(
                ValidationError(
                    ConvertationFileValidationError.FILE_IS_NOT_SUPPORTED_TYPE
                )
            )
        )

        val result = sut.convertAndCollect("user-1", book("book.pdf"))

        assertTrue(result.isLeft())
        assertEquals(FileNotSupported, result.fold(ifLeft = { it }, ifRight = { throw AssertionError() }))
    }

    @Test
    fun `reports an unable to deploy error when the environment deployment fails`() {
        whenever(userEnvironmentService.deployEnvironment("user-1"))
            .thenReturn(Either.Left(UnableDeployError))

        val result = sut.convertAndCollect("user-1", book("book.fb2"))

        assertTrue(result.isLeft())
        assertEquals(UnableDeployEnvironment, result.fold(ifLeft = { it }, ifRight = { throw AssertionError() }))
    }

    @Test
    fun `reports the converter output when the cli fails`() {
        whenever(validationService.validate(any<File>())).thenReturn(Either.Right(Unit))
        whenever(cliRunner.runCli(any<String>(), any<String>(), any<String>(), any<File>()))
            .thenReturn(Either.Left("conversion exploded"))

        val result = sut.convertAndCollect("user-1", book("book.fb2"))

        assertTrue(result.isLeft())
        val error = result.fold(ifLeft = { it }, ifRight = { throw AssertionError() })
        assertEquals(UnableConvertFile("conversion exploded", "fb2-env"), error)
    }

    @Test
    fun `appends the configuration flag when the environment has a config file`() {
        File(environment, "configuration.toml").writeText("key = value")
        whenever(validationService.validate(any<File>())).thenReturn(Either.Right(Unit))
        whenever(cliRunner.runCli(any<String>(), any<String>(), any<String>(), any<File>()))
            .thenAnswer {
                File(environment, "book.azw3").writeText("azw3")
                Either.Right("log")
            }

        sut.convertAndCollect("user-1", book("book.fb2"))

        verify(cliRunner).runCli(
            "/bin/bash",
            "-c",
            "${File(environment, "fb2c").absolutePath} -c configuration.toml convert --to azw3 --verbose book.fb2",
            environment
        )
    }

    @Test
    fun `omits the configuration flag when the environment has no config file`() {
        whenever(validationService.validate(any<File>())).thenReturn(Either.Right(Unit))
        whenever(cliRunner.runCli(any<String>(), any<String>(), any<String>(), any<File>()))
            .thenAnswer {
                File(environment, "book.azw3").writeText("azw3")
                Either.Right("log")
            }

        sut.convertAndCollect("user-1", book("book.fb2"))

        verify(cliRunner).runCli(
            "/bin/bash",
            "-c",
            "${File(environment, "fb2c").absolutePath}  convert --to azw3 --verbose book.fb2",
            environment
        )
    }

    private fun book(name: String) = File(tempDir, name).apply { writeText("source content") }
}
