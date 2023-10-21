package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.configuration.RemoveConfigurationButton
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.UserConfigurationRemovedMessage
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.springframework.stereotype.Service

@Service
class UserConfigurationRemoveListener(
    private val messageSender: NavigatedMessageSender,
    private val userConverterConfigurationService: UserConverterConfigurationService,
) : IncomingMessageEventListener<EventProcessingError>() {

    override fun getOperatingButton() = RemoveConfigurationButton

    override fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError, Unit> =
        userConverterConfigurationService
            .removeConverterConfiguration(event.user.id)

    override fun sendSuccessfulResponse(event: IncomingMessageEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                UserConfigurationRemovedMessage
            )
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}