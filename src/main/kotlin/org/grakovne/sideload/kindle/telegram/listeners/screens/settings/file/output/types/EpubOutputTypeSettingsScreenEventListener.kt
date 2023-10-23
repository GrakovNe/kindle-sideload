package org.grakovne.sideload.kindle.telegram.listeners.screens.settings.file.output.types

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.listeners.IncomingMessageEventListener
import org.grakovne.sideload.kindle.telegram.listeners.screens.main.MainScreenRequestedMessage
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.BackToSettingsButton
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.file.output.EpubOutputButton
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.user.common.OutputFormat
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.springframework.stereotype.Service

@Service
class EpubOutputTypeSettingsScreenEventListener(
    private val userPreferencesService: UserPreferencesService,
    private val messageSender: NavigatedMessageSender
) : IncomingMessageEventListener<EventProcessingError>() {

    override fun getOperatingButtons() = listOf(EpubOutputButton)

    override fun sendSuccessfulResponse(event: IncomingMessageEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                MainScreenRequestedMessage,
                listOf(
                    listOf(BackToSettingsButton),
                )
            )
    }

    override fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError, Unit> =
        userPreferencesService
            .updateOutputFormat(event.user.id, OutputFormat.EPUB).let {
                Either.Right(Unit)
            }
}