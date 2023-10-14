package org.grakovne.sideload.kindle.telegram.command

import arrow.core.Either
import com.pengrad.telegrambot.model.Update
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.user.domain.UserReference

interface TelegramOnMessageCommand {

    fun getKey(): String
    fun getType(): CommandType
    fun getArguments(): String = ""

    fun isAcceptable(update: Update): Boolean = update.message().text().startsWith("/" + getKey())
    fun accept(update: Update, user: UserReference): Either<TelegramUpdateProcessingError, Unit>
}