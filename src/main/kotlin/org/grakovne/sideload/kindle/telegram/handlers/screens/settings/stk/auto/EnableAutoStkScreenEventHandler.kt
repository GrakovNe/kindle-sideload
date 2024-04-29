package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.stk.auto

import arrow.core.Either
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.common.ButtonPressedEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.BackToSettingsButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.springframework.stereotype.Service

@Service
class EnableAutoStkScreenEventHandler(
    private val userPreferencesService: UserPreferencesService,
    private val messageSender: MessageWithNavigationSender,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : ButtonPressedEventHandler<EventProcessingError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(EnableAutoStkButton::class.java)

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                EnableAutoStkSettingsMessage,
                listOf(
                    listOf(BackToSettingsButton),
                    listOf(MainScreenButton)
                )
            )
    }

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> =
        userPreferencesService
            .updateAutomaticStk(event.user.id, true).let {
                Either.Right(Unit)
            }
}