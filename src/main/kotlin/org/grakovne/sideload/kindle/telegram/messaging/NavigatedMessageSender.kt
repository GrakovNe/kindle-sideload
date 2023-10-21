package org.grakovne.sideload.kindle.telegram.messaging

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.sequence
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.Keyboard
import com.pengrad.telegrambot.model.request.KeyboardButton
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove
import com.pengrad.telegrambot.request.SendMessage
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.PreparedButton
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.sideload.kindle.telegram.domain.error.LocalizationError
import org.grakovne.sideload.kindle.telegram.localization.Button
import org.grakovne.sideload.kindle.telegram.localization.Message
import org.grakovne.sideload.kindle.telegram.localization.MessageLocalizationService
import org.grakovne.sideload.kindle.telegram.localization.MessageType
import org.grakovne.sideload.kindle.telegram.localization.NavigationLocalizationService
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service

@Service
class NavigatedMessageSender(
    private val responseSender: ResponseSender,
    private val navigationLocalizationService: NavigationLocalizationService,
    private val messageLocalizationService: MessageLocalizationService
) {

    fun <T : Message> sendResponse(
        chatId: String,
        user: User,
        message: T,
        navigation: List<Button> = emptyList()
    ): Either<EventProcessingError, Unit> {
        val localizedMessage = messageLocalizationService.localize(message, user.language)
        val localizedNavigation =
            navigation.map { navigationLocalizationService.localize(it, user.language) }.sequence()

        return localizedMessage
            .flatMap { preparedMessage ->
                localizedNavigation.flatMap { preparedNavigation ->
                    prepareMessage(chatId, preparedMessage, preparedNavigation).let(responseSender::sendMessage)
                }
            }
            .mapLeft { LocalizationError }

    }

    fun <T : Message> sendResponse(
        origin: Update,
        user: User,
        message: T,
        navigation: List<Button> = emptyList()
    ) = sendResponse(origin.message().chat().id().toString(), user, message, navigation)


    companion object {
        private fun prepareMessage(
            chatId: String,
            message: PreparedMessage,
            navigation: List<PreparedButton>,
            type: MessageType = MessageType.HTML,
        ): SendMessage = SendMessage(chatId, message.text)
            .replyMarkup(navigation.toReplyKeyboard())
            .setParseMode(type)
            .disableWebPagePreview(message.webPagePreview)

        private fun List<PreparedButton>.toReplyKeyboard(): Keyboard {
            if (this.isEmpty()) {
                return ReplyKeyboardRemove()
            }

            return ReplyKeyboardMarkup(this.map { it.toButton() }.toTypedArray())
        }

        private fun PreparedButton.toButton() = KeyboardButton(this.text)

        private fun SendMessage.setParseMode(type: MessageType): SendMessage = when (type) {
            MessageType.PLAIN -> this
            MessageType.HTML -> this.parseMode(ParseMode.HTML)
        }
    }
}