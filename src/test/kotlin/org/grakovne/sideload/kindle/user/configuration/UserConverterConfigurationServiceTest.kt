package org.grakovne.sideload.kindle.user.configuration

import arrow.core.Either
import org.grakovne.sideload.kindle.assets.configuration.default.DefaultConfigurationAssetService
import org.grakovne.sideload.kindle.user.configuration.domain.ValidationError
import org.grakovne.sideload.kindle.user.configuration.validation.ConfigurationValidationError
import org.grakovne.sideload.kindle.user.configuration.validation.ConfigurationValidationRules
import org.grakovne.sideload.kindle.user.configuration.validation.ConfigurationValidationService
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserConverterConfigurationServiceTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var properties: UserConverterConfigurationProperties
    private lateinit var validationService: ConfigurationValidationService
    private lateinit var sut: UserConverterConfigurationService

    @BeforeEach
    fun setUp() {
        properties = UserConverterConfigurationProperties().apply {
            path = File(tempDir, "configs").absolutePath
            fileName = "configuration.zip"
        }
        val rules = ConfigurationValidationRules()
        validationService = ConfigurationValidationService(listOf(rules.`shall be zip file`()))
        sut = UserConverterConfigurationService(properties, validationService, DefaultConfigurationAssetService())
    }

    @Test
    fun `returns the user configuration when it exists`() {
        val asset = File(properties.path, "user-1/${properties.fileName}").apply {
            parentFile.mkdirs()
            writeText("user configuration")
        }

        val result = sut.fetchConverterConfiguration("user-1")

        assertEquals(Either.Right(asset), result)
    }

    @Test
    fun `falls back to the default configuration when the user has none`() {
        val result = sut.fetchConverterConfiguration("user-without-config")

        assertTrue(result.isRight())
        val asset = result.getOrNull()
        assertTrue(asset != null && asset.exists() && asset.extension == "zip")
    }

    @Test
    fun `copies a valid configuration into the user asset`() {
        val user = user("user-1")
        val configuration = File(tempDir, "new-configuration.zip").apply { writeText("new configuration") }

        val result = sut.updateConverterConfiguration(user, configuration)

        assertTrue(result.isRight())
        val asset = result.getOrNull()!!
        assertEquals(File(properties.path, "user-1/${properties.fileName}"), asset)
        assertEquals("new configuration", asset.readText())
    }

    @Test
    fun `rejects a configuration that is not a zip file`() {
        val user = user("user-1")
        val configuration = File(tempDir, "configuration.txt").apply { writeText("not a zip") }

        val result = sut.updateConverterConfiguration(user, configuration)

        assertEquals(Either.Left(ValidationError(ConfigurationValidationError.FILE_IS_NOT_ZIP_FILE)), result)
    }

    @Test
    fun `removes the user configuration asset`() {
        val asset = File(properties.path, "user-1/${properties.fileName}").apply {
            parentFile.mkdirs()
            writeText("configuration")
        }

        val result = sut.removeConverterConfiguration("user-1")

        assertEquals(Either.Right(Unit), result)
        assertTrue(!asset.exists())
    }

    @Test
    fun `removing an absent user configuration is a safe no-op`() {
        val result = sut.removeConverterConfiguration("unknown-user")

        assertEquals(Either.Right(Unit), result)
    }

    private fun user(id: String) = User(
        id = id,
        language = "en",
        type = Type.FREE_USER,
        lastActivityTimestamp = null
    )
}
