package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.model.Update
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventListener
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent

abstract class IncomingMessageEventListener :
    EventListener<IncomingMessageEvent, TelegramUpdateProcessingError>() {

    open fun getDescription(): IncomingMessageDescription? = null

    open fun sendSuccessfulResponse(event: IncomingMessageEvent) = Unit
    open fun sendFailureResponse(event: IncomingMessageEvent) = Unit

    override fun onEvent(event: IncomingMessageEvent) =
        when (event.acceptForListener(getDescription())) {
            true -> logger
                .info { "Received incoming message event for user ${event.user} to ${this.javaClass.simpleName}" }
                .let { processEvent(event) }
                .tap { sendSuccessfulResponse(event) }
                .map { EventProcessingResult.PROCESSED }
                .tap {
                    logger.info {
                        "Incoming message event for user ${event.user} with text ${
                            event.update.message()?.text()
                        } has been successfully processed by ${this.javaClass.simpleName}"
                    }
                }
                .tapLeft { sendFailureResponse(event) }
                .tapLeft {
                    logger.warn {
                        "Incoming message event for user ${event.user} with text ${
                            event.update.message()?.text()
                        } has been failed by ${this.javaClass.simpleName}. See details: $it"
                    }
                }

            false -> Either.Right(EventProcessingResult.SKIPPED)
        }

    protected abstract fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError<TelegramUpdateProcessingError>, Unit>

    companion object {
        private val logger = KotlinLogging.logger { }
    }
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