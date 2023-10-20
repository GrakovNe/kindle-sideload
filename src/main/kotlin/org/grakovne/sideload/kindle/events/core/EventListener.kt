package org.grakovne.sideload.kindle.events.core

import arrow.core.Either
import org.grakovne.sideload.kindle.telegram.domain.error.EventProcessingError

abstract class EventListener<E : Event, T : EventProcessingError> {

    open fun handleEvent(event: Event) = onEvent(event as E)

    abstract fun acceptableEvents(): List<EventType>

    protected abstract fun onEvent(event: E): Either<T, EventProcessingResult>
}