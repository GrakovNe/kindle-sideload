package org.grakovne.sideload.kindle.events.internal

import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventType

data class StkFinishedEvent(
    val userId: String,
    val status: StkFinishedStatus
) : Event(EventType.STK_FINISHED)

enum class StkFinishedStatus {
    SUCCESS,
    FAILED
}