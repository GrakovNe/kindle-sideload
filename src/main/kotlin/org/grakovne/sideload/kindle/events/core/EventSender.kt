package org.grakovne.sideload.kindle.events.core

import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.parallelMap
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class EventSender(@Lazy private val listeners: List<EventListener<out Event, *>>) {

    fun <E : Event> sendEvent(event: E): List<Either<EventProcessingError, EventProcessingResult>> {
        return runBlocking {
            listeners
                .also { logger.debug { "Broadcasting event ${event.eventType}" } }
                .filter { it.acceptableEvents().contains(event.eventType) }
                .parallelMap {
                    it
                        .also { logger.debug { "Found for event ${event.eventType} acceptable processor ${it.javaClass.simpleName}. Sending" } }
                        .handleEvent(event)
                }
        }
    }


    companion object {
        private val logger = KotlinLogging.logger { }
    }
}