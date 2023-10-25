package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.localization.domain.Button
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService

abstract class InputRequiredEventListener<T : EventProcessingError>(
    private val userActivityStateService: UserActivityStateService,
    private val buttonService: ButtonService
) : ReplyingEventListener<ButtonPressedEvent, T>() {

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)

    open fun getRequiredButton(): List<Button> = emptyList()

    override fun onEvent(event: ButtonPressedEvent): Either<T, EventProcessingResult> {
        val requestedButton = userActivityStateService
            .fetchCurrentState(event.user.id)
            ?.let { buttonService.instance(it) }
            ?: return Either.Right(EventProcessingResult.SKIPPED)

        return when (getRequiredButton().any { it == requestedButton }) {
            true -> logger.info { "Received incoming message event for user ${event.user} to ${this.javaClass.simpleName}" }
                .let { processEvent(event) }
                .tap {
                    userActivityStateService.setCurrentState(
                        event.user.id,
                        null
                    )
                }
                .map { EventProcessingResult.PROCESSED }
                .tap { logger.info { "Incoming message event for user ${event.user} has been successfully processed by ${this.javaClass.simpleName}" } }
                .tapLeft { logger.warn { "Incoming message event for user ${event.user} has been failed by ${this.javaClass.simpleName}. See details: $it" } }

            false -> Either.Right(EventProcessingResult.SKIPPED)
        }
    }

    protected abstract fun processEvent(event: ButtonPressedEvent): Either<T, Unit>

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}