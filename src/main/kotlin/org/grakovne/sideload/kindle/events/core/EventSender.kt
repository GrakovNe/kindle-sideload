package org.grakovne.sideload.kindle.events.core

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class EventSender(@Lazy private val listeners: List<EventListener>) {

    fun sendEvent(event: Event) {
        listeners
            .filter { it.acceptableEvents().contains(event.eventType) }
            .forEach { it.onEvent(event) }
    }
}