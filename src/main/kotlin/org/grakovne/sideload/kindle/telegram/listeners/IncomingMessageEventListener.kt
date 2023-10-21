package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.model.Update
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent

abstract class IncomingMessageEventListener<T : EventProcessingError> :
    ReplyingEventListener<IncomingMessageEvent, T>() {

    open fun getDescription(): IncomingMessageDescription? = null

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)

    override fun onEvent(event: IncomingMessageEvent): Either<T, EventProcessingResult> {
        val description = getDescription() ?: return Either.Right(EventProcessingResult.SKIPPED)

        return when (event.acceptForListener(description)) {
            true -> logger
                .info { "Received incoming message event for user ${event.user} to ${this.javaClass.simpleName}" }
                .let { processEvent(event) }
                .map { EventProcessingResult.PROCESSED }
                .tap { logger.info { "Incoming message event for user ${event.user} has been successfully processed by ${this.javaClass.simpleName}" } }
                .tapLeft { logger.warn { "Incoming message event for user ${event.user} has been failed by ${this.javaClass.simpleName}. See details: $it" } }

            false -> Either.Right(EventProcessingResult.SKIPPED)
        }
    }

    protected abstract fun processEvent(event: IncomingMessageEvent): Either<T, Unit>

    protected fun IncomingMessageEvent.acceptForListener(description: IncomingMessageDescription) =
        this.update.hasMessage()
                && this.update.hasSender()
                && this.update.message().text().endsWith(description.key)

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

private fun Update.hasSender() = this.message()?.chat()?.id() != null
private fun Update.hasMessage() = this.message()?.text() != null


data class IncomingMessageDescription(
    val key: String,
    val type: CommandType
)