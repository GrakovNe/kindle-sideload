package org.grakovne.sideload.kindle.telegram.listeners

import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.localization.UserConfigurationRequestedMessage
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.messaging.SimpleMessageSender
import org.grakovne.sideload.kindle.telegram.state.domain.ActivityState
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class UserConfigurationUploadRequestListener(
    private val userActivityStateService: UserActivityStateService,
    private val messageSender: SimpleMessageSender,
) : IncomingMessageEventListener() {

    override fun getDescription(): IncomingMessageDescription = IncomingMessageDescription(
        key = "upload_configuration",
        type = CommandType.UPLOAD_CONFIGURATION_REQUEST
    )

    override fun processEvent(event: IncomingMessageEvent) =
        userActivityStateService
            .setCurrentState(event.user.id, ActivityState.UPLOADING_CONFIGURATION_REQUESTED)
            .mapLeft { EventProcessingError(TelegramUpdateProcessingError.INTERNAL_ERROR) }
            .tap {
                messageSender
                    .sendResponse(
                        event.update,
                        event.user,
                        UserConfigurationRequestedMessage
                    )
            }

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)

}