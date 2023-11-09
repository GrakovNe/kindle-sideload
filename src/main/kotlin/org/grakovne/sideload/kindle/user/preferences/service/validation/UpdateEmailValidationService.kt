package org.grakovne.sideload.kindle.user.preferences.service.validation

import org.grakovne.sideload.kindle.common.validation.ValidationService
import org.springframework.stereotype.Service

@Service
class UpdateEmailValidationService(
    rules: List<UpdateEmailValidationRule>
) : ValidationService<String, UpdateEmailValidationError>(rules)