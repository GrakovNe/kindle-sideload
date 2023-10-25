package org.grakovne.sideload.kindle.telegram.handlers.screens.convertation

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.stk.email.task.domain.InternalError
import org.grakovne.sideload.kindle.stk.email.task.service.TransferEmailTaskService
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.common.ButtonPressedEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionFailedMessage
import org.grakovne.sideload.kindle.telegram.navigation.StkFailedMessage
import org.grakovne.sideload.kindle.telegram.navigation.StkSubmittedMessage
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service

@Service
class BookEmailSideloadRequestHandler(
    private val transferEmailTaskService: TransferEmailTaskService,
    private val userService: UserService,
    private val messageSender: MessageWithNavigationSender,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : ButtonPressedEventHandler<InternalError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(SendConvertedToEmailButton::class.java)

    override fun processEvent(event: ButtonPressedEvent): Either<InternalError, Unit> {
        if (event.update.callbackQuery()?.data() == null) {
            return Either.Left(InternalError)
        }

        val environmentId = Button.fetchButtonPayload(event.update.callbackQuery().data())

        return transferEmailTaskService
            .submitTask(event.user, environmentId = environmentId)
            .mapLeft { InternalError }
    }

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        val user = userService.fetchUser(event.user.id)

        messageSender
            .sendResponse(
                chatId = user.id,
                user = user,
                message = StkSubmittedMessage
            )
    }

    override fun sendFailureResponse(event: ButtonPressedEvent, code: InternalError) {
        val user = userService.fetchUser(event.user.id)

        messageSender
            .sendResponse(
                chatId = user.id,
                user = user,
                message = StkFailedMessage,
                navigation = listOf(
                    listOf(MainScreenButton),
                )
            )
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}