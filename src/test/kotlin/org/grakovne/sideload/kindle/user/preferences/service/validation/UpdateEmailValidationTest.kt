package org.grakovne.sideload.kindle.user.preferences.service.validation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateEmailValidationTest {

    private val rules = UpdateEmailValidationRules()
    private val sut = UpdateEmailValidationService(listOf(rules.`shall be valid email`()))

    @Test
    fun `accepts a valid e-mail address`() {
        val result = sut.validate("user@example.com")

        assertTrue(result.isRight())
    }

    @Test
    fun `rejects a value without an at sign`() {
        val result = sut.validate("user-example-com")

        assertTrue(result.isLeft())
        assertEquals(UpdateEmailValidationError.NOT_VALID_EMAIL, result.swap().orNull()!!.code)
    }

    @Test
    fun `rejects a value without a domain`() {
        val result = sut.validate("user@")

        assertTrue(result.isLeft())
        assertEquals(UpdateEmailValidationError.NOT_VALID_EMAIL, result.swap().orNull()!!.code)
    }

    @Test
    fun `rejects an empty value`() {
        val result = sut.validate("")

        assertTrue(result.isLeft())
        assertEquals(UpdateEmailValidationError.NOT_VALID_EMAIL, result.swap().orNull()!!.code)
    }
}
