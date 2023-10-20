package org.grakovne.sideload.kindle.telegram.listeners

import org.grakovne.sideload.kindle.common.ifTrue
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventListener
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.domain.error.EventProcessingError

abstract class ReplyingEventListener<E : Event, T : EventProcessingError> : EventListener<E, T>() {

    open fun sendSuccessfulResponse(event: E) = Unit
    open fun sendFailureResponse(event: E, code: T) = Unit

    override fun handleEvent(event: Event) =
        super.handleEvent(event as E)
            .tap {
                (it == EventProcessingResult.PROCESSED).ifTrue { sendSuccessfulResponse(event) }
            }
            .tapLeft { sendFailureResponse(event, it) }
}