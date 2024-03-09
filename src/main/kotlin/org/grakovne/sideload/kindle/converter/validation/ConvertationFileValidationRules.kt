package org.grakovne.sideload.kindle.converter.validation

import arrow.core.Either
import org.grakovne.sideload.kindle.common.validation.ValidationError
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ConvertationFileValidationRules {

    @Bean
    fun `shall be a supported type`() = ConvertationFileValidationRule { file ->
        when (supportedFileTypes.contains(file.extension)) {
            true -> Either.Right(Unit)
            false -> Either.Left(ValidationError(ConvertationFileValidationError.FILE_IS_NOT_SUPPORTED_TYPE))
        }
    }

    companion object {
        private val supportedFileTypes = listOf("fb2", "zip")
    }
}

enum class ConvertationFileValidationError {
    FILE_IS_NOT_SUPPORTED_TYPE
}