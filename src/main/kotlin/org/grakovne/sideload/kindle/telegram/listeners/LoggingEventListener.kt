package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventListener
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.internal.LogLevel.Companion.isWorseOrEqualThan
import org.grakovne.sideload.kindle.events.internal.LoggingEvent
import org.grakovne.sideload.kindle.telegram.ConfigurationProperties
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.user.UserReferenceService
import org.springframework.stereotype.Service

@Service
class LoggingEventListener(
    private val bot: TelegramBot,
    private val properties: ConfigurationProperties,
    private val userReferenceService: UserReferenceService
) : EventListener<LoggingEvent, TelegramUpdateProcessingError> {
    override fun acceptableEvents(): List<EventType> = listOf(EventType.LOG_SENT)

    override fun onEvent(event: Event): Either<EventProcessingError<TelegramUpdateProcessingError>, Unit> {
        return when (event) {
            is LoggingEvent -> processLoggingEvent(event)
            else -> Either.Left(EventProcessingError(TelegramUpdateProcessingError.INTERNAL_ERROR))
        }
    }

    private fun processLoggingEvent(event: LoggingEvent): Either<EventProcessingError<TelegramUpdateProcessingError>, Unit> {
        if (event.level.isWorseOrEqualThan(properties.level)) {
            userReferenceService.fetchSuperUsers()
                .forEach { bot.execute(SendMessage(it.id, event.toMessage()).parseMode(ParseMode.HTML)) }
        }

        return Either.Right(Unit)
    }


    private fun LoggingEvent.toMessage(): String = """
    <i>[Admin] Logging Event Occurred!
    
    Status: ${this.level}
    Message: ${this.message}</i>
""".trimIndent()
}
