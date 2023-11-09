package org.grakovne.sideload.kindle.user.preferences.service.validation

import arrow.core.Either
import org.grakovne.sideload.kindle.common.validation.ValidationError
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UpdateEmailValidationRules {

    @Bean
    fun `shall be valid email`() = UpdateEmailValidationRule {
        val emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"

        when (it.matches(emailRegex.toRegex())) {
            true -> Either.Right(Unit)
            false -> Either.Left(ValidationError(UpdateEmailValidationError.NOT_VALID_EMAIL))
        }
    }
}

enum class UpdateEmailValidationError {
    NOT_VALID_EMAIL
}