package org.grakovne.sideload.kindle.telegram.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import org.grakovne.sideload.kindle.common.ifTrue
import org.grakovne.sideload.kindle.telegram.ConfigurationProperties
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Collections
import java.util.concurrent.Executors

@Service
class AsyncLoggerAppender(
    private val bot: TelegramBot,
    private val userService: UserService,
    private val configurationProperties: ConfigurationProperties
) : AppenderBase<ILoggingEvent>() {

    private val executor = Executors.newSingleThreadExecutor()

    private val loggingEvents: MutableList<String> = Collections.synchronizedList(mutableListOf<String>())
    private var nextSendingAfter = Instant.now().plus(configurationProperties.loggingTimeout)

    override fun append(event: ILoggingEvent?) {
        if (null == event) {
            return
        }

        if (!event.level.isGreaterOrEqual(configurationProperties.level)) {
            return
        }

        userService
            .fetchSuperUsers()
            .forEach { user ->
                executor.execute {
                    appendLog(event)
                    Instant.now().isAfter(nextSendingAfter).ifTrue {
                        bot.execute(SendMessage(user.id, flush()))
                        nextSendingAfter = Instant.now().plus(configurationProperties.loggingTimeout)
                    }

                }
            }
    }

    private fun appendLog(event: ILoggingEvent) = loggingEvents.add("${event.level}: ${event.formattedMessage}")

    private fun flush() = synchronized(loggingEvents) {
        loggingEvents.joinToString("\n").also { loggingEvents.clear() }
    }
}