package org.grakovne.sideload.kindle.telegram.handlers.screens.settings

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.common.ButtonPressedEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestSettingButton
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class SettingsScreenRequestedEventHandler(
    private val messageSender: NavigatedMessageSender,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : ButtonPressedEventHandler<EventProcessingError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(BackToSettingsButton, RequestSettingButton)

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                SettingsScreenRequestedMessage,
                listOf(
                    listOf(OutputFileTypeSettingsScreenButton, StkSettingsScreenButton),
                    listOf(ConverterConfigurationSettingsScreenButton, DebugModeSettingScreenButton),
                    listOf(MainScreenButton),
                )
            )
    }

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> = Either.Right(Unit)

}