package org.grakovne.sideload.kindle.user.configuration.validation

import org.grakovne.sideload.kindle.common.validation.ValidationService
import org.springframework.stereotype.Service
import java.io.File

@Service
class ConfigurationValidationService(
    rules: List<ConfigurationValidationRule>
) : ValidationService<File, ConfigurationValidationError>(rules)