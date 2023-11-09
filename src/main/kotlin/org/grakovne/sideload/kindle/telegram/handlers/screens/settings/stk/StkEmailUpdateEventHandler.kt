package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.stk

import arrow.core.Either
import org.grakovne.sideload.kindle.common.FileUploadFailedError
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.common.InputRequiredEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.BackToSettingsButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.grakovne.sideload.kindle.user.preferences.service.validation.UpdateEmailValidationError
import org.springframework.stereotype.Service

@Service
class StkEmailUpdateEventHandler(
    userActivityStateService: UserActivityStateService,
    buttonService: ButtonService,
    private val messageSender: MessageWithNavigationSender,
    private val userPreferencesService: UserPreferencesService
) : InputRequiredEventHandler<EventProcessingError>(userActivityStateService, buttonService) {

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                UpdateEmailUpdatedMessage,
                listOf(
                    listOf(BackToSettingsButton),
                    listOf(MainScreenButton),
                )
            )
    }

    override fun sendFailureResponse(event: ButtonPressedEvent, code: EventProcessingError) {
        messageSender
            .sendResponse(
                event.update,
                event.user,
                UpdateEmailUpdateFailedMessage(UpdateEmailValidationError.NOT_VALID_EMAIL),
                listOf(
                    listOf(BackToSettingsButton),
                    listOf(MainScreenButton),
                    )
            )
    }

    override fun onEvent(event: ButtonPressedEvent) = event
        .update
        .message()
        ?.text()
        ?.let { super.onEvent(event) }
        ?: Either.Right(EventProcessingResult.SKIPPED)

    override fun getRequiredButton(): List<Button> = listOf(UpdateStkEmailPromptButton)

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> {
        val email = event.update.message()?.text() ?: return Either.Right(Unit)

        return userPreferencesService
            .updateEmail(event.user.id, email)
    }
}