package org.grakovne.sideload.kindle.events.internal

import org.grakovne.sideload.kindle.converter.StkFinished
import org.grakovne.sideload.kindle.events.core.Event

data class StkFinishedEvent(
    val userId: String,
    val status: StkFinishedStatus
) : Event(StkFinished)

enum class StkFinishedStatus {
    SUCCESS,
    FAILED
}