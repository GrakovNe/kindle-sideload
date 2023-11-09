package org.grakovne.sideload.kindle.user.preferences.service.validation

import org.grakovne.sideload.kindle.common.validation.ValidationRule

fun interface UpdateEmailValidationRule : ValidationRule<String, UpdateEmailValidationError>