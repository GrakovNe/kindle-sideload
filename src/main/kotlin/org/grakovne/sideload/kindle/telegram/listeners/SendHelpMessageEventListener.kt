package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.internal.LogLevel
import org.grakovne.sideload.kindle.events.internal.LoggingEvent
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.messaging.Help
import org.grakovne.sideload.kindle.telegram.messaging.HelpMessageSender
import org.springframework.stereotype.Service

@Service
class SendHelpMessageEventListener(
    private val incomingMessageEventListeners: List<IncomingMessageEventListener>,
    private val eventSender: EventSender,
    private val helpMessageSender: HelpMessageSender
) : IncomingMessageEventListener() {

    override fun getDescription() = IncomingMessageDescription("help", CommandType.SEND_HELP)

    override fun processEvent(event: IncomingMessageEvent) =
        incomingMessageEventListeners
            .map { listener -> listener.getDescription().let { Help(it.key, it.type) } }
            .let { helpMessageSender.sendResponse(event.update, event.user, it) }
            .mapLeft { EventProcessingError(it) }
            .tap {
                eventSender.sendEvent(
                    LoggingEvent(
                        LogLevel.DEBUG,
                        "Help text was sent in response on origin message: ${event.update.message().text()}"
                    )
                )
            }

    override fun acceptableEvents() = listOf(EventType.INCOMING_MESSAGE)

    fun forceProcessEvent(event: IncomingMessageEvent) = processEvent(event)

}