package org.grakovne.sideload.kindle.telegram.listeners.screens.stk

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import mu.KotlinLogging
import org.grakovne.sideload.kindle.transferring.email.domain.InternalError
import org.grakovne.sideload.kindle.common.FileUploadFailedError
import org.grakovne.sideload.kindle.common.TaskQueueingError
import org.grakovne.sideload.kindle.common.configuration.FileUploadProperties
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.listeners.ButtonPressedEventListener
import org.grakovne.sideload.kindle.telegram.listeners.screens.convertation.SendConvertedToEmailButton
import org.grakovne.sideload.kindle.telegram.listeners.screens.project.info.ProjectInfoMessage
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.transferring.email.service.TransferEmailTaskService
import org.springframework.stereotype.Service

@Service
class BookEmailSideloadRequestListener(
    private val transferEmailTaskService: TransferEmailTaskService,
    private val messageSender: NavigatedMessageSender,
    private val buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : ButtonPressedEventListener<InternalError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(SendConvertedToEmailButton())

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender.sendResponse(
            origin = event.update,
            user = event.user,
            message = ProjectInfoMessage
        )
    }

    override fun processEvent(event: ButtonPressedEvent): Either<InternalError, Unit> {
        val environmentId = buttonService
            .fetchButtonPayload(event.update.callbackQuery().data())
            ?: return Either.Left(InternalError)

        return transferEmailTaskService
            .submitTask(event.user, environmentId = environmentId)
            .mapLeft { InternalError }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}