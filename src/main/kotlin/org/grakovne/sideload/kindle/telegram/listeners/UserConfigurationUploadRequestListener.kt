package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.domain.error.UnknownError
import org.grakovne.sideload.kindle.telegram.messaging.SimpleMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.UserConfigurationRequestedMessage
import org.grakovne.sideload.kindle.telegram.state.domain.ActivityState
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class UserConfigurationUploadRequestListener(
    private val userActivityStateService: UserActivityStateService,
    private val messageSender: SimpleMessageSender,
) : IncomingMessageEventListener<EventProcessingError>() {

    override fun sendSuccessfulResponse(event: IncomingMessageEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                UserConfigurationRequestedMessage
            )
    }

    override fun getDescription(): IncomingMessageDescription = IncomingMessageDescription(
        key = "upload_configuration",
        type = CommandType.UPLOAD_CONFIGURATION_REQUEST
    )

    override fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError, Unit> =
        userActivityStateService
            .setCurrentState(event.user.id, ActivityState.UPLOADING_CONFIGURATION_REQUESTED)
            .mapLeft { UnknownError }

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}