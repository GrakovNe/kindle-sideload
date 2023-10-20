package org.grakovne.sideload.kindle.telegram.listeners

import mu.KotlinLogging
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.springframework.stereotype.Service

@Service
class UnprocessedIncomingEventService(
    private val sender: SendHelpMessageListener
) {

    fun handle(incomingMessageEvent: IncomingMessageEvent) = sender.processEvent(incomingMessageEvent)

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}