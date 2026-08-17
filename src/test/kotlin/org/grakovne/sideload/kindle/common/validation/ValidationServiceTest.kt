package org.grakovne.sideload.kindle.common.validation

import arrow.core.Either
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private enum class SomeValidationError {
    NOT_A_ZIP, TOO_LONG
}

class ValidationServiceTest {

    private class AlwaysValid : ValidationRule<String, SomeValidationError> {
        override fun apply(sut: String) = Either.Right(Unit)
    }

    private class RejectsLongStrings(private val limit: Int) : ValidationRule<String, SomeValidationError> {
        override fun apply(sut: String) =
            if (sut.length > limit) Either.Left(ValidationError(SomeValidationError.TOO_LONG)) else Either.Right(Unit)
    }

    private class Svc : ValidationService<String, SomeValidationError>(
        rules = listOf(AlwaysValid(), RejectsLongStrings(3))
    )

    private val sut = Svc()

    @Test
    fun `passes when all rules accept`() {
        assertTrue(sut.validate("abc").isRight())
    }

    @Test
    fun `fails with the code of the violated rule`() {
        val result = sut.validate("abcd")

        assertTrue(result.isLeft())
        val error = result.fold(ifLeft = { it }, ifRight = { throw AssertionError("expected a failure") })
        assertEquals(SomeValidationError.TOO_LONG, error.code)
    }

    @Test
    fun `passes trivially with no rules at all`() {
        val empty = object : ValidationService<String, SomeValidationError>(rules = emptyList()) {}
        assertTrue(empty.validate("anything").isRight())
    }
}
