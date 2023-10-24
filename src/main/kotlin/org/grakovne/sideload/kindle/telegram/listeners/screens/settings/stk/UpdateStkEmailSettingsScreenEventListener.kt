package org.grakovne.sideload.kindle.telegram.listeners.screens.settings.stk

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.listeners.ButtonPressedEventListener
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.BackToSettingsButton
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.common.OutputFormat
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.springframework.stereotype.Service

@Service
class UpdateStkEmailSettingsScreenEventListener(
    private val userPreferencesService: UserPreferencesService,
    private val messageSender: NavigatedMessageSender,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : ButtonPressedEventListener<EventProcessingError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(UpdateStkEmailButton)

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                UpdateEmailPromptMessage,
                listOf(
                    listOf(BackToSettingsButton),
                    listOf(MainScreenButton)
                )
            )
    }

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> =
        userPreferencesService
            .updateOutputFormat(event.user.id, OutputFormat.EPUB).let {
                Either.Right(Unit)
            }
}