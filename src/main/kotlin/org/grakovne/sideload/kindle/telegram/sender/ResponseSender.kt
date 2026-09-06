package org.grakovne.sideload.kindle.telegram.sender

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.BaseResponse
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

    fun editMessage(message: EditMessageText): Either<UnableSendResponse, Unit> {
        val response = bot.execute(message)

        return when {
            response.isOk -> Either.Right(Unit)

            response.isNotModified() -> Either.Right(Unit)
                .also { logger.debug { "The message edit was a no-op, treating it as a success" } }

            else -> Either.Left(UnableSendResponse)
                .also { logger.error { "Unable to edit the message due to: ${response.description()}" } }
        }
    }

    fun answerCallbackQuery(callbackQueryId: String) {
        val response = bot.execute(AnswerCallbackQuery(callbackQueryId))

        if (!response.isOk) {
            // QUERY_ID_INVALID is expected on redelivered or expired callbacks, no user impact
            logger.debug { "Unable to answer the callback query $callbackQueryId due to: ${response.description()}" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }

        private const val MESSAGE_NOT_MODIFIED_ERROR_CODE = 400
        private const val MESSAGE_NOT_MODIFIED_DESCRIPTION = "message is not modified"

        private fun BaseResponse.isNotModified(): Boolean =
            errorCode() == MESSAGE_NOT_MODIFIED_ERROR_CODE &&
                description()?.contains(MESSAGE_NOT_MODIFIED_DESCRIPTION, ignoreCase = true) == true
    }
}
