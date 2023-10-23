package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.fetchText
import org.grakovne.sideload.kindle.telegram.localization.domain.Button

abstract class IncomingMessageEventListener<T : EventProcessingError> :
    ReplyingEventListener<IncomingMessageEvent, T>() {

    open fun getOperatingButtons(): List<Button> = emptyList()

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)

    override fun onEvent(event: IncomingMessageEvent) =
        when (getOperatingButtons().any { event.acceptForListener(it) }) {
            true -> logger
                .info { "Received incoming message event for user ${event.user} to ${this.javaClass.simpleName}" }
                .let { processEvent(event) }
                .map { EventProcessingResult.PROCESSED }
                .tap { logger.info { "Incoming message event for user ${event.user} has been successfully processed by ${this.javaClass.simpleName}" } }
                .tapLeft { logger.warn { "Incoming message event for user ${event.user} has been failed by ${this.javaClass.simpleName}. See details: $it" } }

            false -> Either.Right(EventProcessingResult.SKIPPED)
        }

    protected abstract fun processEvent(event: IncomingMessageEvent): Either<T, Unit>

    protected fun IncomingMessageEvent.acceptForListener(button: Button) =
        this.update.fetchText() == button.javaClass.simpleName

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}