package org.grakovne.sideload.kindle.user.configuration.validation

import arrow.core.Either
import org.grakovne.sideload.kindle.common.validation.ValidationError
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ConfigurationValidationRules {

    @Bean
    fun `shall be zip file`() = ConfigurationValidationRule {
        when (it.extension == "zip") {
            true -> Either.Right(Unit)
            false -> Either.Left(ValidationError(ConfigurationValidationError.FILE_IS_NOT_ZIP_FILE))
        }
    }
}

enum class ConfigurationValidationError {
    FILE_IS_NOT_ZIP_FILE
}