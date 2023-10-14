package org.grakovne.sideload.kindle.events.core

import arrow.core.Either

interface EventListener<E: Event, T> {
    fun acceptableEvents(): List<EventType>

    fun onEvent(event: Event): Either<EventProcessingError<T>, EventProcessingResult>
}