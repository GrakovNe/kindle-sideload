package org.grakovne.sideload.kindle.telegram.messaging

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.sideload.kindle.telegram.domain.error.UnableSendResponse
import org.grakovne.swiftbot.localization.MessageType
import org.springframework.stereotype.Service

@Service
abstract class MessageSender(private val bot: TelegramBot) {

    protected fun sendRawMessage(
        chatId: String,
        message: PreparedMessage,
        type: MessageType = MessageType.HTML,
    ): Either<EventProcessingError, Unit> {
        val isMessageSent = SendMessage(chatId, message.text)
            .setParseMode(type)
            .disableWebPagePreview(message.webPagePreview)
            .let { bot.execute(it).isOk }

        return when (isMessageSent) {
            true -> Either.Right(Unit)
            false -> Either.Left(UnableSendResponse)
        }
    }

    protected fun sendRawMessage(
        origin: Update,
        message: PreparedMessage,
        type: MessageType = MessageType.HTML,
    ) = sendRawMessage(origin.message().chat().id().toString(), message, type)
}

private fun SendMessage.setParseMode(type: MessageType): SendMessage = when (type) {
    MessageType.PLAIN -> this
    MessageType.HTML -> this.parseMode(ParseMode.HTML)
}