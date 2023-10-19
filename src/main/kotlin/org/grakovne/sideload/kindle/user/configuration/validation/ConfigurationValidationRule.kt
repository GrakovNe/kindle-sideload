package org.grakovne.sideload.kindle.user.configuration.validation

import org.grakovne.sideload.kindle.common.validation.ValidationRule
import java.io.File

fun interface ConfigurationValidationRule : ValidationRule<File, ConfigurationValidationError>