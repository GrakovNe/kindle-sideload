package org.grakovne.sideload.kindle.common.validation

import arrow.core.Either

fun interface ValidationRule<T, E : Enum<out E>> {

    fun apply(sut: T): Either<ValidationError<E>, Unit>
}