package org.grakovne.sideload.kindle.user.configuration.validation

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurationValidationTest {

    private val rule = ConfigurationValidationRules()
    private val sut = ConfigurationValidationService(listOf(rule.`shall be zip file`()))

    @Test
    fun `accepts a zip file`() {
        val result = sut.validate(File("configuration.zip"))

        assertTrue(result.isRight())
    }

    @Test
    fun `rejects a file with another extension`() {
        val result = sut.validate(File("configuration.txt"))

        assertTrue(result.isLeft())
        assertEquals(ConfigurationValidationError.FILE_IS_NOT_ZIP_FILE, result.swap().orNull()!!.code)
    }
}
