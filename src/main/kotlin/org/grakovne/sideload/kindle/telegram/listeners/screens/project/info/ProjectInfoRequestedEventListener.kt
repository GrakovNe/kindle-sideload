package org.grakovne.sideload.kindle.telegram.listeners.screens.project.info

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.listeners.IncomingMessageEventListener
import org.grakovne.sideload.kindle.telegram.listeners.screens.main.MainScreenRequestedMessage
import org.grakovne.sideload.kindle.telegram.listeners.screens.main.RequestProjectInfoButton
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.springframework.stereotype.Service

@Service
class ProjectInfoRequestedEventListener(
    private val messageSender: NavigatedMessageSender
) : IncomingMessageEventListener<EventProcessingError>() {

    override fun getOperatingButtons() = listOf(RequestProjectInfoButton)

    override fun sendSuccessfulResponse(event: IncomingMessageEvent) {
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

    override fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError, Unit> = Either.Right(Unit)

}