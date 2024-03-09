package org.grakovne.sideload.kindle.converter.validation

import org.grakovne.sideload.kindle.common.validation.ValidationService
import org.springframework.stereotype.Service
import java.io.File

@Service
class ConvertationFileValidationService(
    rules: List<ConvertationFileValidationRule>
) : ValidationService<File, ConvertationFileValidationError>(rules)