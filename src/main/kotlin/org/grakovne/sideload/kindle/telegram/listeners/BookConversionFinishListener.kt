package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendDocument
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventListener
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import org.grakovne.sideload.kindle.events.internal.UserEnvironmentUnnecessaryEvent
import org.grakovne.sideload.kindle.localization.FileConvertarionFailed
import org.grakovne.sideload.kindle.localization.FileConvertarionSuccess
import org.grakovne.sideload.kindle.telegram.domain.error.NewEventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.error.UndescribedError
import org.grakovne.sideload.kindle.telegram.messaging.SimpleMessageSender
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service

@Service
class BookConversionFinishListener(
    private val bot: TelegramBot,
    private val messageSender: SimpleMessageSender,
    private val userService: UserService,
    private val eventSender: EventSender
) : EventListener<ConvertationFinishedEvent, NewEventProcessingError>(),
    SilentEventListener {

    override fun acceptableEvents(): List<EventType> = listOf(EventType.CONVERTATION_FINISHED)

    override fun sendSuccessfulResponse(event: ConvertationFinishedEvent) {
        val user = userService.fetchUser(event.userId)

        messageSender
            .sendResponse(
                chatId = user.id,
                user = user,
                message = FileConvertarionSuccess(event.log)
            )
            .map {
                event
                    .output
                    .map { SendDocument(event.userId, it) }
                    .map { bot.execute(it) }
            }
            .also {
                eventSender.sendEvent(
                    UserEnvironmentUnnecessaryEvent(
                        environmentId = event.environmentId
                    )
                )
            }
    }

    override fun sendFailureResponse(event: ConvertationFinishedEvent, code: NewEventProcessingError) {
        val user = userService.fetchUser(event.userId)

        messageSender
            .sendResponse(
                chatId = user.id,
                user = user,
                message = FileConvertarionFailed(event.log)
            )
            .mapLeft { EventProcessingError(it) }
            .map { EventProcessingResult.PROCESSED }
            .also {
                eventSender.sendEvent(
                    UserEnvironmentUnnecessaryEvent(
                        environmentId = event.environmentId
                    )
                )
            }
    }

    override fun onEvent(event: ConvertationFinishedEvent): Either<EventProcessingError<NewEventProcessingError>, EventProcessingResult> =
        when (event.status) {
            ConvertationFinishedStatus.SUCCESS -> Either.Right(EventProcessingResult.PROCESSED)
            ConvertationFinishedStatus.FAILED -> Either.Left(EventProcessingError(UndescribedError))
        }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}