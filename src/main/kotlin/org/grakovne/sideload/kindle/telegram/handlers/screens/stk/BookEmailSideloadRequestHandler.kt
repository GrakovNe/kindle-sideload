package org.grakovne.sideload.kindle.telegram.handlers.screens.stk

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.common.ButtonPressedEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.convertation.SendConvertedToEmailButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.project.info.ProjectInfoMessage
import org.grakovne.sideload.kindle.telegram.localization.domain.Button
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.stk.email.task.domain.InternalError
import org.grakovne.sideload.kindle.stk.email.task.service.TransferEmailTaskService
import org.springframework.stereotype.Service

@Service
class BookEmailSideloadRequestHandler(
    private val transferEmailTaskService: TransferEmailTaskService,
    private val messageSender: NavigatedMessageSender,
    private val buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : ButtonPressedEventHandler<InternalError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(SendConvertedToEmailButton())

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender.sendResponse(
            origin = event.update,
            user = event.user,
            message = ProjectInfoMessage
        )
    }

    override fun processEvent(event: ButtonPressedEvent): Either<InternalError, Unit> {
        if (event.update.callbackQuery()?.data() == null) {
            return Either.Left(InternalError)
        }

        val environmentId = Button.fetchButtonPayload(event.update.callbackQuery().data())

        return transferEmailTaskService
            .submitTask(event.user, environmentId = environmentId)
            .mapLeft { InternalError }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}