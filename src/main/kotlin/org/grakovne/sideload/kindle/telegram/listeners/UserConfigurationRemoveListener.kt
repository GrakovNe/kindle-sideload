package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.localization.UserConfigurationRemovedMessage
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.domain.error.NewEventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.error.UndescribedError
import org.grakovne.sideload.kindle.telegram.messaging.SimpleMessageSender
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.springframework.stereotype.Service

@Service
class UserConfigurationRemoveListener(
    private val messageSender: SimpleMessageSender,
    private val userConverterConfigurationService: UserConverterConfigurationService,
) : IncomingMessageEventListener<NewEventProcessingError>() {

    override fun getDescription(): IncomingMessageDescription = IncomingMessageDescription(
        key = "remove_configuration",
        type = CommandType.REMOVE_CONFIGURATION_REQUEST
    )

    override fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError<NewEventProcessingError>, Unit> =
        userConverterConfigurationService
            .removeConverterConfiguration(event.user.id)
            .mapLeft { EventProcessingError(UndescribedError) }

    override fun sendSuccessfulResponse(event: IncomingMessageEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                UserConfigurationRemovedMessage
            )
    }

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}