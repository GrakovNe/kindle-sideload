package org.grakovne.sideload.kindle.telegram.handlers

import mu.KotlinLogging
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.screens.convertation.BookConversionRequestHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.MainScreenRequestedEventHandler
import org.springframework.stereotype.Service

@Service
class UnprocessedIncomingEventService(
    private val mainScreenRequestedEventListener: MainScreenRequestedEventHandler,
    private val bookConversionRequestHandler: BookConversionRequestHandler
) {

    fun handle(event: ButtonPressedEvent) {
        event
            .update
            .message()
            ?.document()
            ?.let { bookConversionRequestHandler.processEvent(event) }
            ?: mainScreenRequestedEventListener.sendSuccessfulResponse(event)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}