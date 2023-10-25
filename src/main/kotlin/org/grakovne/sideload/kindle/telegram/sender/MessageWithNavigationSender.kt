package org.grakovne.sideload.kindle.telegram.sender

import arrow.core.Either
import arrow.core.sequence
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.Keyboard
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove
import com.pengrad.telegrambot.request.SendMessage
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.PreparedButton
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.sideload.kindle.telegram.domain.error.LocalizationError
import org.grakovne.sideload.kindle.telegram.fetchUserId
import org.grakovne.sideload.kindle.telegram.localization.MessageLocalizationService
import org.grakovne.sideload.kindle.telegram.localization.NavigationLocalizationService
import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.grakovne.sideload.kindle.common.navigation.domain.Button.Companion.buildQualifiedName
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.telegram.localization.template.MessageType
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service

@Service
class MessageWithNavigationSender(
    private val responseSender: ResponseSender,
    private val navigationLocalizationService: NavigationLocalizationService,
    private val messageLocalizationService: MessageLocalizationService
) {

    fun <T : Message> sendResponse(
        chatId: String,
        user: User,
        message: T,
        navigation: List<List<Button>> = emptyList()
    ): Either<EventProcessingError, Unit> {
        val localizedMessage = messageLocalizationService
            .localize(message, user.language)
            .fold(
                ifLeft = {
                    logger.error { "Unable to localize message $message due to: $it" }
                    return Either.Left(LocalizationError)
                },
                ifRight = { it }
            )

        val localizedNavigation =
            navigation
                .map { row ->
                    row
                        .map { button ->
                            navigationLocalizationService.localize(button, user.language)
                                .map { button to it }
                        }
                        .sequence()
                }
                .sequence()
                .fold(
                    ifLeft = {
                        logger.error { "Unable to localize navigation $message due to: $it" }
                        return Either.Left(LocalizationError)
                    },
                    ifRight = { it }
                )

        return prepareMessage(chatId, localizedMessage, localizedNavigation).let { responseSender.sendMessage(it) }
    }

    private fun prepareMessage(
        chatId: String,
        message: PreparedMessage,
        navigation: List<List<Pair<Button, PreparedButton>>>,
        type: MessageType = MessageType.HTML,
    ): SendMessage {
        return SendMessage(chatId, message.text)
            .replyMarkup(navigation.toReplyKeyboard())
            .setParseMode(type)
            .disableWebPagePreview(message.enablePreview.not())
            .entities()
    }

    fun <T : Message> sendResponse(
        origin: Update,
        user: User,
        message: T,
        navigation: List<List<Button>> = emptyList()
    ) = sendResponse(origin.fetchUserId(), user, message, navigation)

    private fun List<List<Pair<Button, PreparedButton>>>.toReplyKeyboard(): Keyboard {
        if (this.isEmpty()) {
            return ReplyKeyboardRemove()
        }

        val layout: List<List<InlineKeyboardButton>> = this
            .map { row -> row.map { it.toButton() } }

        return InlineKeyboardMarkup(*layout.map { it.toTypedArray() }.toTypedArray())
    }

    private fun Pair<Button, PreparedButton>.toButton(): InlineKeyboardButton {
        val (button, preparedButton) = this

        return InlineKeyboardButton(preparedButton.text)
            .callbackData(button.buildQualifiedName())
    }


    companion object {

        private val logger = KotlinLogging.logger { }

        private fun SendMessage.setParseMode(type: MessageType): SendMessage = when (type) {
            MessageType.PLAIN -> this
            MessageType.HTML -> this.parseMode(ParseMode.HTML)
        }
    }
}