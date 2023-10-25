package org.grakovne.sideload.kindle.telegram.sender

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import org.grakovne.sideload.kindle.telegram.domain.error.UnableSendResponse
import org.springframework.stereotype.Service

@Service
class ResponseSender(val bot: TelegramBot) {

    fun sendMessage(message: SendMessage) = when (bot.execute(message).isOk) {
        true -> Either.Right(Unit)
        false -> Either.Left(UnableSendResponse)
    }
}