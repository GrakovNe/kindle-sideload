package org.grakovne.sideload.kindle.common.validation

open class ValidationError<E : Enum<out E>>(
    val code: E
)