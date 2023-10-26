package org.grakovne.sideload.kindle.telegram.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import org.grakovne.sideload.kindle.telegram.ConfigurationProperties
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class AsyncLoggerAppender(
    private val bot: TelegramBot,
    private val userService: UserService,
    private val configurationProperties: ConfigurationProperties
) : AppenderBase<ILoggingEvent>() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun append(event: ILoggingEvent?) {
        if (null == event) {
            return
        }

        if (!event.level.isGreaterOrEqual(configurationProperties.level)) {
            return
        }

        userService
            .fetchSuperUsers()
            .forEach { user -> executor.execute { bot.execute(SendMessage(user.id, event.formattedMessage)) } }
    }
}