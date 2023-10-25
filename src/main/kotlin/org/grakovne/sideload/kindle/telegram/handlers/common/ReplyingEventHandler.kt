package org.grakovne.sideload.kindle.telegram.handlers.common

import org.grakovne.sideload.kindle.common.ifTrue
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventHandler
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult

abstract class ReplyingEventHandler<E : Event, T : EventProcessingError> : EventHandler<E, T>() {

    open fun sendSuccessfulResponse(event: E) = Unit
    open fun sendFailureResponse(event: E, code: T) = Unit

    @Suppress("UNCHECKED_CAST")
    override fun handleEvent(event: Event) =
        super.handleEvent(event as E)
            .tap { (it == EventProcessingResult.PROCESSED).ifTrue { sendSuccessfulResponse(event) } }
            .tapLeft { sendFailureResponse(event, it) }
}