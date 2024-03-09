package org.grakovne.sideload.kindle.converter.validation

import org.grakovne.sideload.kindle.common.validation.ValidationRule
import java.io.File

fun interface ConvertationFileValidationRule  : ValidationRule<File, ConvertationFileValidationError>