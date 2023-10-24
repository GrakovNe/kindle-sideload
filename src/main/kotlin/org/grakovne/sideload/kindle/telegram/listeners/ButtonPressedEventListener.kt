package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.model.Update
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.localization.domain.Button
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService

abstract class ButtonPressedEventListener<T : EventProcessingError>(
    private val buttonService: ButtonService,
    private val userActivityStateService: UserActivityStateService
) : ReplyingEventListener<ButtonPressedEvent, T>() {

    open fun getOperatingButtons(): List<Button> = emptyList()

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)

    override fun onEvent(event: ButtonPressedEvent) =
        when (getOperatingButtons().any { event.acceptForListener(it) }) {
            true -> logger
                .info { "Received incoming message event for user ${event.user} to ${this.javaClass.simpleName}" }
                .also {
                    userActivityStateService.setCurrentState(
                        event.user.id,
                        event.update.fetchPressedButton()?.let { buttonService.fetchButtonName(it) })
                }
                .let { processEvent(event) }
                .map { EventProcessingResult.PROCESSED }
                .tap { logger.info { "Incoming message event for user ${event.user} has been successfully processed by ${this.javaClass.simpleName}" } }
                .tapLeft { logger.warn { "Incoming message event for user ${event.user} has been failed by ${this.javaClass.simpleName}. See details: $it" } }

            false -> Either.Right(EventProcessingResult.SKIPPED)
        }

    protected abstract fun processEvent(event: ButtonPressedEvent): Either<T, Unit>

    private fun Update.fetchPressedButton(): Button? {
        if (null != this.callbackQuery()?.data()) {
            return buttonService.fetchButtonForName(this.callbackQuery().data())
        }

        return null
    }

    protected fun ButtonPressedEvent.acceptForListener(button: Button): Boolean {
        val pressedButton = this.update.fetchPressedButton()
        return this.update.fetchPressedButton() == button
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}