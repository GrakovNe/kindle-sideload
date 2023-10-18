package org.grakovne.sideload.kindle.events.core

import mu.KotlinLogging
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class EventSender(@Lazy private val listeners: List<EventListener<*, *>>) {

    fun <E : Event> sendEvent(event: E) = listeners
        .also { logger.debug { "Broadcasting event ${event.eventType}" } }
        .filter { it.acceptableEvents().contains(event.eventType) }
        .map {
            it
                .also { logger.debug { "Found for event ${event.eventType} acceptable processor ${it.javaClass.simpleName}. Sending" } }
                .onEvent(event)
        }


    companion object {
        private val logger = KotlinLogging.logger { }
    }
}