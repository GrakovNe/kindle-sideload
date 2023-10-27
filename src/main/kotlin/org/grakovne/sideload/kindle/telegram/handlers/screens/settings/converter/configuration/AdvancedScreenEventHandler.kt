package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.converter.configuration

import arrow.core.Either
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.common.ButtonPressedEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.AdvancedSettingsScreenButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.BackToSettingsButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.DebugModeSettingScreenButton
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.springframework.stereotype.Service

@Service
class AdvancedScreenEventHandler(
    private val userConverterConfigurationService: UserConverterConfigurationService,
    private val messageSender: MessageWithNavigationSender,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : ButtonPressedEventHandler<EventProcessingError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(AdvancedSettingsScreenButton::class.java)

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        val userConfiguration = userConverterConfigurationService
            .fetchConverterConfiguration(event.user.id)
            .fold(
                ifLeft = { null },
                ifRight = { it }
            )

        messageSender
            .sendResponse(
                event.update,
                event.user,
                AdvancedConfigurationMessage,
                listOf(
                    listOf(UploadConfigurationButton, RemoveConfigurationButton),
                    userConfiguration?.let { listOf(FetchUserConfigurationButton) } ?: emptyList(),
                    listOf(FetchDefaultConfigurationButton),
                    listOf(DebugModeSettingScreenButton),
                    listOf(BackToSettingsButton),
                )
            )
    }

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> = Either.Right(Unit)
}