package org.grakovne.sideload.kindle.events.core

import arrow.core.Either

abstract class EventListener<E : Event, T> {

    fun handleEvent(event: Event) = onEvent(event as E)

    abstract fun acceptableEvents(): List<EventType>

    protected abstract fun onEvent(event: E): Either<EventProcessingError<T>, EventProcessingResult>
}