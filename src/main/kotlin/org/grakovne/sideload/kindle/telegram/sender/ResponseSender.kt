package org.grakovne.sideload.kindle.telegram.sender

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import mu.KotlinLogging
import org.grakovne.sideload.kindle.telegram.domain.error.UnableSendResponse
import org.springframework.stereotype.Service

@Service
class ResponseSender(val bot: TelegramBot) {

    fun sendMessage(message: SendMessage): Either<UnableSendResponse, Unit> {
        val response = bot.execute(message)

        return when (response.isOk) {
            true -> Either.Right(Unit)
            false -> Either.Left(UnableSendResponse)
                .also { logger.error { "Unable to send the message due to: ${response.description()}" } }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}