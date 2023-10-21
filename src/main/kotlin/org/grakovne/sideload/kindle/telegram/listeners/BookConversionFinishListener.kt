package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendDocument
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.parallelMap
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import org.grakovne.sideload.kindle.events.internal.UserEnvironmentUnnecessaryEvent

import org.grakovne.sideload.kindle.telegram.domain.error.UnknownError
import org.grakovne.sideload.kindle.telegram.messaging.SimpleMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionFailed
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionSuccess
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service

@Service
class BookConversionFinishListener(
    private val bot: TelegramBot,
    private val messageSender: SimpleMessageSender,
    private val userService: UserService,
    private val eventSender: EventSender
) : ReplyingEventListener<ConvertationFinishedEvent, EventProcessingError>(),
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
                runBlocking {
                    event
                        .output
                        .map { SendDocument(event.userId, it) }
                        .parallelMap { bot.execute(it) }
                }
            }
            .also {
                eventSender.sendEvent(
                    UserEnvironmentUnnecessaryEvent(
                        environmentId = event.environmentId
                    )
                )
            }
    }

    override fun sendFailureResponse(event: ConvertationFinishedEvent, code: EventProcessingError) {
        val user = userService.fetchUser(event.userId)

        messageSender
            .sendResponse(
                chatId = user.id,
                user = user,
                message = FileConvertarionFailed(event.log)
            )
            .map { EventProcessingResult.PROCESSED }
            .also {
                eventSender.sendEvent(
                    UserEnvironmentUnnecessaryEvent(
                        environmentId = event.environmentId
                    )
                )
            }
    }

    override fun onEvent(event: ConvertationFinishedEvent): Either<EventProcessingError, EventProcessingResult> =
        when (event.status) {
            ConvertationFinishedStatus.SUCCESS -> Either.Right(EventProcessingResult.PROCESSED)
            ConvertationFinishedStatus.FAILED -> Either.Left(UnknownError)
        }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}