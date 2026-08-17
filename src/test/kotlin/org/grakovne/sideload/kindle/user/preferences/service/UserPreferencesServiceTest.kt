package org.grakovne.sideload.kindle.user.preferences.service

import arrow.core.Either
import org.grakovne.sideload.kindle.user.common.OutputFormat
import org.grakovne.sideload.kindle.user.configuration.domain.EmailNotValidError
import org.grakovne.sideload.kindle.user.preferences.repository.UserPreferencesRepository
import org.grakovne.sideload.kindle.user.preferences.service.validation.UpdateEmailValidationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@DataJpaTest
class UserPreferencesServiceTest {

    @Autowired
    lateinit var repository: UserPreferencesRepository

    private val validationService: UpdateEmailValidationService = mock()
    private lateinit var sut: UserPreferencesService

    @BeforeEach
    fun setUp() {
        whenever(validationService.validate(any())).thenReturn(Either.Right(Unit))
        sut = UserPreferencesService(validationService, repository)
    }

    @Test
    fun `creates a user with the default preferences when absent`() {
        val preferences = sut.fetchPreferences("user-1")

        assertEquals("user-1", preferences.userId)
        assertEquals(OutputFormat.EPUB, preferences.outputFormat)
        assertEquals(false, preferences.debugMode)
        assertEquals(false, preferences.automaticStk)
        assertEquals(null, preferences.email)
    }

    @Test
    fun `returns the stored preferences when present`() {
        val stored = sut.fetchPreferences("user-1")
        repository.save(stored.copy(outputFormat = OutputFormat.AZW3))

        val preferences = sut.fetchPreferences("user-1")

        assertEquals(OutputFormat.AZW3, preferences.outputFormat)
        assertEquals(1L, repository.count())
    }

    @Test
    fun `updates the email when the value is valid`() {
        sut.updateEmail("user-1", "user@example.com")

        assertEquals("user@example.com", sut.fetchPreferences("user-1").email)
    }

    @Test
    fun `reports an invalid email and keeps the previous value`() {
        whenever(validationService.validate(any())).thenReturn(Either.Left(
            org.grakovne.sideload.kindle.common.validation.ValidationError(
                org.grakovne.sideload.kindle.user.preferences.service.validation.UpdateEmailValidationError.NOT_VALID_EMAIL
            )
        ))

        val result = sut.updateEmail("user-1", "not-an-email")

        assertEquals(Either.Left(EmailNotValidError), result)
        assertEquals(null, sut.fetchPreferences("user-1").email)
    }

    @Test
    fun `updates the output format`() {
        sut.updateOutputFormat("user-1", OutputFormat.KEPUB)

        assertEquals(OutputFormat.KEPUB, sut.fetchPreferences("user-1").outputFormat)
    }

    @Test
    fun `updates the debug mode`() {
        sut.updateDebugMode("user-1", true)

        assertEquals(true, sut.fetchPreferences("user-1").debugMode)
    }

    @Test
    fun `updates the automatic stk flag`() {
        sut.updateAutomaticStk("user-1", true)

        assertEquals(true, sut.fetchPreferences("user-1").automaticStk)
    }

}
