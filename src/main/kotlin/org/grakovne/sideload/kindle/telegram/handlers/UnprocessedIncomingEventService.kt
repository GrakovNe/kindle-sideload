package org.grakovne.sideload.kindle.telegram.handlers

import mu.KotlinLogging
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.MainScreenRequestedEventHandler
import org.springframework.stereotype.Service

@Service
class UnprocessedIncomingEventService(
    private val mainScreenRequestedEventListener: MainScreenRequestedEventHandler
) {

    fun handle(incomingMessageEvent: ButtonPressedEvent) =
        mainScreenRequestedEventListener.sendSuccessfulResponse(incomingMessageEvent)

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}