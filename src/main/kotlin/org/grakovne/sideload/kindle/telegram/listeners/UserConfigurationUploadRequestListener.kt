package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.converter.configuration.UploadConfigurationButton
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.navigation.UserConfigurationRequestedMessage
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class UserConfigurationUploadRequestListener(
    private val messageSender: NavigatedMessageSender,
    private val buttonService: ButtonService,
    private val userActivityStateService: UserActivityStateService,
) : ButtonPressedEventListener<EventProcessingError>(buttonService, userActivityStateService) {

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                UserConfigurationRequestedMessage
            )
    }

    override fun getOperatingButtons() = listOf(UploadConfigurationButton)

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> = Either.Right(Unit)

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}