package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.messaging.SimpleMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.MainScreenRequestedMessage
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.configuration.domain.InternalError
import org.springframework.stereotype.Service

@Service
class MainScreenRequestedEventListener(
    private val messageSender: SimpleMessageSender,
    private val userActivityStateService: UserActivityStateService
) : IncomingMessageEventListener<EventProcessingError>() {

    override fun getDescription() = IncomingMessageDescription("main_screen", CommandType.MAIN_SCREEN_REQUESTED)

    override fun sendSuccessfulResponse(event: IncomingMessageEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                MainScreenRequestedMessage
            )
    }

    override fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError, Unit> =
        userActivityStateService
            .dropCurrentState(event.user.id)
            .mapLeft { InternalError }
}