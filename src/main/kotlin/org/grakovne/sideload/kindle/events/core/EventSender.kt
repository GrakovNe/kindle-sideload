package org.grakovne.sideload.kindle.events.core

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class EventSender(@Lazy private val listeners: List<EventListener<*, *>>) {

    fun <E : Event> sendEvent(event: E) = listeners
        .filter { it.acceptableEvents().contains(event.eventType) }
        .map { it.onEvent(event) }
}