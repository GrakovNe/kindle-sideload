package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import org.grakovne.sideload.kindle.events.core.EventListener
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.internal.LogLevel.Companion.isWorseOrEqualThan
import org.grakovne.sideload.kindle.events.internal.LoggingEvent
import org.grakovne.sideload.kindle.telegram.ConfigurationProperties
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service

@Service
class LoggingEventListener(
    private val bot: TelegramBot,
    private val properties: ConfigurationProperties,
    private val userService: UserService
) : EventListener<LoggingEvent, EventProcessingError>() {
    override fun acceptableEvents(): List<EventType> = listOf(EventType.LOG_SENT)

    override fun onEvent(event: LoggingEvent): Either.Right<EventProcessingResult> {
        if (event.level.isWorseOrEqualThan(properties.level)) {
            userService.fetchSuperUsers()
                .forEach { bot.execute(SendMessage(it.id, event.toMessage()).parseMode(ParseMode.HTML)) }
        }

        return Either.Right(EventProcessingResult.PROCESSED)
    }


    private fun LoggingEvent.toMessage(): String = """
    <i>[Admin] Logging Event Occurred!
    
    Status: ${this.level}
    Message: ${this.message}</i>
""".trimIndent()
}
