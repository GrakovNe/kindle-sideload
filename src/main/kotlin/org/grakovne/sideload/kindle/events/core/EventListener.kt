package org.grakovne.sideload.kindle.events.core

import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventType

interface EventListener {
    fun acceptableEvents(): List<EventType>

    fun onEvent(event: Event)
}