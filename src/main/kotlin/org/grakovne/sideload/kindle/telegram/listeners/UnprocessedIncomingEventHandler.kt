package org.grakovne.sideload.kindle.telegram.listeners

import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.springframework.stereotype.Service

@Service
class UnprocessedIncomingEventHandler(
    private val sender: SendHelpMessageEventListener
) {

    fun handle(incomingMessageEvent: IncomingMessageEvent) = sender.forceProcessEvent(incomingMessageEvent)
}