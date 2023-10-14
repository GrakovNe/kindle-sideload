package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventListener
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent

interface IncomingMessageEventListener : EventListener<IncomingMessageEvent, TelegramUpdateProcessingError> {

    override fun onEvent(event: Event) =
        when (event) {
            is IncomingMessageEvent -> when (event.update.message().text().startsWith("/" + getDescription().key)) {
                true -> processEvent(event)
                false -> Either.Right(Unit)
            }

            else -> Either.Left(EventProcessingError(TelegramUpdateProcessingError.INTERNAL_ERROR))
        }

    fun getDescription(): IncomingMessageDescription

    fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError<TelegramUpdateProcessingError>, Unit>
}

data class IncomingMessageDescription(
    val key: String,
    val type: CommandType
)