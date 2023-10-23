package org.grakovne.sideload.kindle.telegram.listeners.screens.conversation.prompt

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.listeners.ButtonPressedEventListener
import org.grakovne.sideload.kindle.telegram.listeners.screens.main.MainScreenRequestedMessage
import org.grakovne.sideload.kindle.telegram.listeners.screens.main.RequestConvertationPromptButton
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class ConversationPromptRequestedEventListener(
    private val messageSender: NavigatedMessageSender,
    private val buttonService: ButtonService,
    private val userActivityStateService: UserActivityStateService,
) : ButtonPressedEventListener<EventProcessingError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(RequestConvertationPromptButton)

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                MainScreenRequestedMessage,
                listOf(
                    listOf(MainScreenButton)
                )
            )
    }

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> = Either.Right(Unit)

}