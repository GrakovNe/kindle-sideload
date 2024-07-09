package org.grakovne.sideload.kindle.events.core

import arrow.core.Either

abstract class EventHandler<E : Event, T : EventProcessingError> {

    @Suppress("UNCHECKED_CAST")
    open fun handleEvent(event: Event) = onEvent(event as E)

    abstract fun acceptableEvents(): List<EventType>

    protected abstract fun onEvent(event: E): Either<T, EventProcessingResult>
}