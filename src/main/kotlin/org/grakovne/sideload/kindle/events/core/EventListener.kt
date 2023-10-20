package org.grakovne.sideload.kindle.events.core

import arrow.core.Either
import org.grakovne.sideload.kindle.common.ifTrue
import org.grakovne.sideload.kindle.telegram.domain.error.NewEventProcessingError

abstract class EventListener<E : Event, T : NewEventProcessingError> {

    open fun sendSuccessfulResponse(event: E) = Unit
    open fun sendFailureResponse(event: E, code: T) = Unit

    fun handleEvent(event: Event) =
        onEvent(event as E)
            .tap {
                (it == EventProcessingResult.PROCESSED).ifTrue { sendSuccessfulResponse(event) }
            }
            .tapLeft { sendFailureResponse(event, it) }

    abstract fun acceptableEvents(): List<EventType>

    protected abstract fun onEvent(event: E): Either<T, EventProcessingResult>
}