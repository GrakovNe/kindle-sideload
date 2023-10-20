package org.grakovne.sideload.kindle.telegram.listeners

import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.localization.UserConfigurationRemovedMessage
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.messaging.SimpleMessageSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.springframework.stereotype.Service

@Service
class UserConfigurationRemoveListener(
    private val messageSender: SimpleMessageSender,
    private val userConverterConfigurationService: UserConverterConfigurationService,
) : IncomingMessageEventListener() {

    override fun getDescription(): IncomingMessageDescription = IncomingMessageDescription(
        key = "remove_configuration",
        type = CommandType.REMOVE_CONFIGURATION_REQUEST
    )

    override fun sendSuccessfulResponse(event: IncomingMessageEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                UserConfigurationRemovedMessage
            )
    }

    override fun processEvent(event: IncomingMessageEvent) =
        userConverterConfigurationService
            .removeConverterConfiguration(event.user.id)
            .mapLeft { EventProcessingError(TelegramUpdateProcessingError.INTERNAL_ERROR) }

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}