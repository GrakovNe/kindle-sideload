package org.grakovne.sideload.kindle.telegram.listeners.screens.settings.stk

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.listeners.screens.ButtonResolvingEventListener
import org.grakovne.sideload.kindle.telegram.listeners.screens.main.MainScreenRequestedMessage
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.BackToSettingsButton
import org.grakovne.sideload.kindle.telegram.localization.domain.Button
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.springframework.stereotype.Service

@Service
class StkEmailUpdateEventListener(
    userActivityStateService: UserActivityStateService,
    buttonService: ButtonService,
    private val messageSender: NavigatedMessageSender,
    private val userPreferencesService: UserPreferencesService
) : ButtonResolvingEventListener<EventProcessingError>(userActivityStateService, buttonService) {

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                MainScreenRequestedMessage,
                listOf(
                    listOf(UpdateStkEmailButton),
                    listOf(BackToSettingsButton),
                )
            )
    }

    override fun getResolvingButton(): List<Button> = listOf(UpdateStkEmailButton)

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> {
        val email = event.update.message()?.text() ?: return Either.Right(Unit)

        return userPreferencesService
            .updateEmail(event.user.id, email)
            .let { Either.Right(Unit) }
    }
}