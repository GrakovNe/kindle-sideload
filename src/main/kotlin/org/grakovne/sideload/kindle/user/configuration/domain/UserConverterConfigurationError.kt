package org.grakovne.sideload.kindle.user.configuration.domain

import org.grakovne.sideload.kindle.user.configuration.validation.ConfigurationValidationError

sealed interface UserConverterConfigurationError

data object ConfigurationNotFoundError : UserConverterConfigurationError
data object UnableUpdateConfigurationError : UserConverterConfigurationError
data class ValidationError(val code: ConfigurationValidationError) : UserConverterConfigurationError