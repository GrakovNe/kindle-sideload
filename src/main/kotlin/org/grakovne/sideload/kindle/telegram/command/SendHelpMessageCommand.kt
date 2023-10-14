package org.grakovne.sideload.kindle.telegram.command

import com.pengrad.telegrambot.model.Update
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.LogLevel
import org.grakovne.sideload.kindle.events.internal.LoggingEvent
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.messaging.Help
import org.grakovne.sideload.kindle.telegram.messaging.HelpMessageSender
import org.grakovne.sideload.kindle.user.domain.UserReference
import org.springframework.stereotype.Service

@Service
class SendHelpMessageCommand(
    private val onMessageCommands: List<TelegramOnMessageCommand>,
    private val eventSender: EventSender,
    private val helpMessageSender: HelpMessageSender
) : TelegramOnMessageCommand {

    override fun getKey(): String = "help"
    override fun getType() = CommandType.SEND_HELP

    override fun accept(
        update: Update,
        user: UserReference
    ) = onMessageCommands
        .map { Help(it.getKey(), it.getType(), it.getArguments()) }
        .let { helpMessageSender.sendResponse(update, user, it) }
        .tap {
            eventSender.sendEvent(
                LoggingEvent(
                    LogLevel.DEBUG,
                    "Help text was sent in response on origin message: ${update.message().text()}"
                )
            )
        }
}