package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.model.Update
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventListener
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent

abstract class IncomingMessageEventListener : EventListener<IncomingMessageEvent, TelegramUpdateProcessingError> {

    open fun getDescription(): IncomingMessageDescription? = null

    override fun onEvent(event: Event) =
        when (event is IncomingMessageEvent && event.acceptForListener(getDescription())) {
            true -> processEvent(event).map { EventProcessingResult.PROCESSED }
            false -> Either.Right(EventProcessingResult.SKIPPED)
        }

    protected abstract fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError<TelegramUpdateProcessingError>, Unit>
}

private fun Update.hasSender() = this.message()?.chat()?.id() != null
private fun Update.hasMessage() = this.message()?.text() != null

private fun IncomingMessageEvent.acceptForListener(description: IncomingMessageDescription?) = this.update.hasMessage()
        && this.update.hasSender()
        && this.update.message().text().startsWith("/" + description?.key)

data class IncomingMessageDescription(
    val key: String,
    val type: CommandType
)