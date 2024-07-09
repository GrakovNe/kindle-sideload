package org.grakovne.sideload.kindle.events.internal

import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.core.StkFinished

data class StkFinishedEvent(
    val userId: String,
    val status: StkFinishedStatus
) : Event(StkFinished)

enum class StkFinishedStatus {
    SUCCESS,
    FAILED
}