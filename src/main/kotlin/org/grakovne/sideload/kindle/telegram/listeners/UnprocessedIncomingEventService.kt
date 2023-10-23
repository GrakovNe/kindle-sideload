package org.grakovne.sideload.kindle.telegram.listeners

import mu.KotlinLogging
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.listeners.screens.main.MainScreenRequestedEventListener
import org.springframework.stereotype.Service

@Service
class UnprocessedIncomingEventService(
    private val mainScreenRequestedEventListener: MainScreenRequestedEventListener
) {

    fun handle(incomingMessageEvent: ButtonPressedEvent) =
        mainScreenRequestedEventListener.sendSuccessfulResponse(incomingMessageEvent)

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}