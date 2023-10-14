package org.grakovne.sideload.kindle.telegram.command

import com.pengrad.telegrambot.model.Update
import org.springframework.stereotype.Service

@Service
class UnknownMessageCommandProcessingService(
    private val helpMessageCommand: SendHelpMessageCommand
) {

    fun findCommand(update: Update): TelegramOnMessageCommand {
        return helpMessageCommand
    }
}