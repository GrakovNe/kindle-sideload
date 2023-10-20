package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import arrow.core.flatMap
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
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.messaging.SimpleMessageSender
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service

@Service
class BookConversionFinishListener(
    private val bot: TelegramBot,
    private val messageSender: SimpleMessageSender,
    private val userService: UserService,
    private val eventSender: EventSender
) : EventListener<ConvertationFinishedEvent, TelegramUpdateProcessingError>(),
    SilentEventListener {

    override fun acceptableEvents(): List<EventType> = listOf(EventType.CONVERTATION_FINISHED)

    override fun onEvent(event: ConvertationFinishedEvent): Either<EventProcessingError<TelegramUpdateProcessingError>, EventProcessingResult> {
        val user = userService.fetchUser(event.userId) ?: return Either.Left(
            EventProcessingError(
                TelegramUpdateProcessingError.TARGET_USER_DISAPPEAR
            )
        )

        return when (event.status) {
            ConvertationFinishedStatus.SUCCESS -> responseSuccess(user, event)
            ConvertationFinishedStatus.FAILED -> responseFailed(user, event)
        }

    }

    private fun responseFailed(
        user: User,
        event: ConvertationFinishedEvent
    ): Either<EventProcessingError<TelegramUpdateProcessingError>, EventProcessingResult> = messageSender
        .sendResponse(
            chatId = user.id,
            user = user,
            message = FileConvertarionFailed(event.log)
        )
        .mapLeft { EventProcessingError(it) }
        .map { EventProcessingResult.PROCESSED }

    private fun responseSuccess(
        user: User,
        event: ConvertationFinishedEvent
    ) = messageSender
        .sendResponse(
            chatId = user.id,
            user = user,
            message = FileConvertarionSuccess(event.log)
        )
        .mapLeft { EventProcessingError(TelegramUpdateProcessingError.RESPONSE_NOT_SENT) }
        .map {
            event
                .output
                .map { SendDocument(event.userId, it) }
                .map { bot.execute(it) }
        }
        .flatMap {
            it.find { item -> !item.isOk }
                ?.let { Either.Left(EventProcessingError(TelegramUpdateProcessingError.RESPONSE_NOT_SENT)) }
                ?: Either.Right(EventProcessingResult.PROCESSED)
        }
        .also {
            eventSender.sendEvent(
                UserEnvironmentUnnecessaryEvent(
                    environmentId = event.environmentId
                )
            )
        }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}