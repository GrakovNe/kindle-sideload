package org.grakovne.sideload.kindle.telegram.handlers.screens

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.internal.StkFinishedEvent
import org.grakovne.sideload.kindle.events.internal.StkFinishedStatus
import org.grakovne.sideload.kindle.telegram.domain.error.UnknownError
import org.grakovne.sideload.kindle.telegram.handlers.common.ReplyingEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.navigation.StkFailedMessage
import org.grakovne.sideload.kindle.telegram.navigation.StkSuccessMessage
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigation
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service

@Service
class StkFinishHandler(
    private val messageSender: MessageWithNavigation,
    private val userService: UserService
) : ReplyingEventHandler<StkFinishedEvent, EventProcessingError>() {

    override fun acceptableEvents(): List<EventType> = listOf(EventType.STK_FINISHED)

    override fun sendSuccessfulResponse(event: StkFinishedEvent) {
        val user = userService.fetchUser(event.userId)

        messageSender
            .sendResponse(
                chatId = user.id,
                user = user,
                message = StkSuccessMessage,
                navigation = listOf(
                    listOf(MainScreenButton),
                )
            )
    }

    override fun sendFailureResponse(event: StkFinishedEvent, code: EventProcessingError) {
        val user = userService.fetchUser(event.userId)

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

    override fun onEvent(event: StkFinishedEvent): Either<EventProcessingError, EventProcessingResult> =
        when (event.status) {
            StkFinishedStatus.SUCCESS -> Either.Right(EventProcessingResult.PROCESSED)
            StkFinishedStatus.FAILED -> Either.Left(UnknownError)
        }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}