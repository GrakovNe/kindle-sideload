package org.grakovne.sideload.kindle.telegram.messaging

import arrow.core.flatMap
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import org.grakovne.sideload.kindle.localization.Message
import org.grakovne.sideload.kindle.localization.MessageLocalizationService
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.user.domain.UserReference
import org.springframework.stereotype.Service

@Service
class SimpleMessageSender(
    bot: TelegramBot,
    private val localizationService: MessageLocalizationService
) : MessageSender(bot) {

    fun <T : Message> sendResponse(
        chatId: String,
        userReference: UserReference,
        message: T
    ) = localizationService
        .localize(message, userReference.provideLanguage())
        .mapLeft { TelegramUpdateProcessingError.INTERNAL_ERROR }
        .flatMap { sendRawMessage(chatId, it) }

    fun <T : Message> sendResponse(
        origin: Update,
        userReference: UserReference,
        message: T
    ) = sendResponse(origin.message().chat().id().toString(), userReference, message)
}