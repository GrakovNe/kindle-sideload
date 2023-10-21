package org.grakovne.sideload.kindle.telegram.listeners.screens.settings

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.listeners.IncomingMessageDescription
import org.grakovne.sideload.kindle.telegram.listeners.IncomingMessageEventListener
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.springframework.stereotype.Service

@Service
class SettingsScreenRequestedEventListener(
    private val messageSender: NavigatedMessageSender
) : IncomingMessageEventListener<EventProcessingError>() {

    override fun getDescription() = IncomingMessageDescription("Settings", CommandType.SETTINGS_SCREEN_REQUESTED)

    override fun sendSuccessfulResponse(event: IncomingMessageEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                SettingsScreenRequestedMessage,
                listOf(
                    listOf(MainScreenButton),
                    listOf(OutputFileTypeSettingsScreenButton, SendToKindleSettingsScreenButton),
                    listOf(DebugModeSettingScreenButton, ConverterConfigurationSettingsScreenButton)
                )
            )
    }

    override fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError, Unit> = Either.Right(Unit)

}