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
import java.time.Duration

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

    /**
     * [sendMessage] with the failure classified, for callers that must decide whether to retry.
     */
    fun sendWithOutcome(message: SendMessage): Either<SendFailure, Unit> {
        val response = bot.execute(message)

        if (response.isOk) {
            return Either.Right(Unit)
        }

        val failure = classify(response)
        // INFO, not WARN: a campaign hits plenty of users who deleted the bot, and the appender
        // forwards everything at or above telegram.level to the super users.
        logger.info { "Unable to send the message due to: ${response.description()}. Classified as $failure" }
        return Either.Left(failure)
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
        private const val FORBIDDEN_ERROR_CODE = 403
        private const val TOO_MANY_REQUESTS_ERROR_CODE = 429
        private val UNDELIVERABLE_DESCRIPTIONS = listOf(
            "bot was blocked by the user",
            "bot can't initiate a chat",
            "chat not found",
            "user is deactivated",
        )
        private const val DEFAULT_RETRY_AFTER_SECONDS = 60L

        private fun BaseResponse.isNotModified(): Boolean =
            errorCode() == MESSAGE_NOT_MODIFIED_ERROR_CODE &&
                description()?.contains(MESSAGE_NOT_MODIFIED_DESCRIPTION, ignoreCase = true) == true

        private fun classify(response: BaseResponse): SendFailure = when {
            response.errorCode() == FORBIDDEN_ERROR_CODE -> SendFailure.Undeliverable

            UNDELIVERABLE_DESCRIPTIONS.any {
                response.description()?.contains(it, ignoreCase = true) == true
            } -> SendFailure.Undeliverable

            response.errorCode() == TOO_MANY_REQUESTS_ERROR_CODE -> SendFailure.RateLimited(retryAfter(response))

            else -> SendFailure.Retryable
        }

        /** Telegram only sends `retry_after` for 429; a missing one still needs a sane backoff. */
        private fun retryAfter(response: BaseResponse): Duration =
            response.parameters()?.retryAfter()
                ?.toLong()
                ?.let { Duration.ofSeconds(it) }
                ?: Duration.ofSeconds(DEFAULT_RETRY_AFTER_SECONDS)
    }
}
