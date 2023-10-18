package org.grakovne.sideload.kindle.telegram.messaging

import arrow.core.flatMap
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import org.grakovne.sideload.kindle.localization.Message
import org.grakovne.sideload.kindle.localization.MessageLocalizationService
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service

@Service
class SimpleMessageSender(
    bot: TelegramBot,
    private val localizationService: MessageLocalizationService
) : MessageSender(bot) {

    fun <T : Message> sendResponse(
        chatId: String,
        user: User,
        message: T
    ) = localizationService
        .localize(message, user.language)
        .mapLeft { TelegramUpdateProcessingError.LOCALIZATION_ERROR }
        .flatMap { sendRawMessage(chatId, it) }

    fun <T : Message> sendResponse(
        origin: Update,
        user: User,
        message: T
    ) = sendResponse(origin.message().chat().id().toString(), user, message)
}