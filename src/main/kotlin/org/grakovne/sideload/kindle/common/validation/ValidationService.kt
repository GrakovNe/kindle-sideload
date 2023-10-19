package org.grakovne.sideload.kindle.common.validation

import arrow.core.sequence
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.parallelMap

abstract class ValidationService<T, E : Enum<out E>>(
    private val rules: List<ValidationRule<T, E>>
) {

    fun validate(sut: T) = runBlocking { rules.parallelMap { it.apply(sut) }.sequence().map { } }
}