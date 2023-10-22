package org.grakovne.sideload.kindle.telegram.listeners.screens.settings.file.output

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.listeners.IncomingMessageEventListener
import org.grakovne.sideload.kindle.telegram.listeners.screens.main.MainScreenRequestedMessage
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.BackToSettingsButton
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.DebugModeSettingScreenButton
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.OutputFileTypeSettingsScreenButton
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.springframework.stereotype.Service

@Service
class OutputTypeSettingsScreenEventListener(
    private val messageSender: NavigatedMessageSender
) : IncomingMessageEventListener<EventProcessingError>() {

    override fun getOperatingButtons() = listOf(OutputFileTypeSettingsScreenButton)

    override fun sendSuccessfulResponse(event: IncomingMessageEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                MainScreenRequestedMessage,
                listOf(
                    listOf(EpubOutputButton, KEpubOutputButton, Awz3ModeButton),
                    listOf(BackToSettingsButton),
                )
            )
    }

    override fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError, Unit> = Either.Right(Unit)
}