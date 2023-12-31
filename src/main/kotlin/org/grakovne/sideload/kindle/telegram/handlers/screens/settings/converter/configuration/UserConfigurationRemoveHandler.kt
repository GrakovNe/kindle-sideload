package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.converter.configuration

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.common.ButtonPressedEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.BackToSettingsButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.navigation.UserConfigurationRemovedMessage
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.springframework.stereotype.Service

@Service
class UserConfigurationRemoveHandler(
    private val messageSender: MessageWithNavigationSender,
    private val userConverterConfigurationService: UserConverterConfigurationService,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : ButtonPressedEventHandler<EventProcessingError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(RemoveConfigurationButton::class.java)

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> =
        userConverterConfigurationService
            .removeConverterConfiguration(event.user.id)

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                UserConfigurationRemovedMessage,
                listOf(
                    listOf(BackToSettingsButton),
                    listOf(MainScreenButton)
                )
            )
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}