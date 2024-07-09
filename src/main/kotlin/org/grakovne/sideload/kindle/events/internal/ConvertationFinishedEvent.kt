package org.grakovne.sideload.kindle.events.internal

import org.grakovne.sideload.kindle.converter.ConvertationError
import org.grakovne.sideload.kindle.converter.ConvertationFinished
import org.grakovne.sideload.kindle.events.core.Event
import java.io.File

data class ConvertationFinishedEvent(
    val userId: String,
    val status: ConvertationFinishedStatus,
    val log: String,
    val output: List<File>,
    val environmentId: String?,
    val failureReason: ConvertationError? = null
) : Event(ConvertationFinished)

enum class ConvertationFinishedStatus {
    SUCCESS,
    FAILED
}