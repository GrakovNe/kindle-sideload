package org.grakovne.sideload.kindle.telegram.sender

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import com.pengrad.telegrambot.model.LinkPreviewOptions
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.Keyboard
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.grakovne.sideload.kindle.common.navigation.domain.Button.Companion.buildQualifiedName
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.ConfigurationProperties
import org.grakovne.sideload.kindle.telegram.domain.PreparedButton
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.sideload.kindle.telegram.domain.error.LocalizationError
import org.grakovne.sideload.kindle.telegram.fetchUserId
import org.grakovne.sideload.kindle.telegram.localization.MessageLocalizationService
import org.grakovne.sideload.kindle.telegram.localization.NavigationLocalizationService
import org.grakovne.sideload.kindle.telegram.localization.template.MessageType
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service

@Service
class MessageWithNavigationSender(
    private val responseSender: ResponseSender,
    private val navigationLocalizationService: NavigationLocalizationService,
    private val messageLocalizationService: MessageLocalizationService,
    private val configurationProperties: ConfigurationProperties
) {

    fun <T : Message> sendResponse(
        chatId: String,
        user: User,
        message: T,
        navigation: List<List<Button>>
    ): Either<EventProcessingError, Unit> = localize(message, user, navigation)
        .flatMap { (preparedMessage, preparedNavigation) ->
            responseSender.sendMessage(buildSendMessage(chatId, preparedMessage, preparedNavigation))
        }

    fun <T : Message> sendResponse(
        origin: Update,
        user: User,
        message: T,
        navigation: List<List<Button>> = emptyList()
    ): Either<EventProcessingError, Unit> = localize(message, user, navigation)
        .flatMap { (preparedMessage, preparedNavigation) ->
            when (val target = fetchResponseTarget(origin)) {
                is ResponseTarget.Edit ->
                    responseSender
                        .editMessage(buildEditMessage(target.chatId, target.messageId, preparedMessage, preparedNavigation))
                        .onLeft {
                            logger.warn {
                                "Unable to edit the message ${target.messageId} in chat ${target.chatId}, " +
                                    "falling back to a new message"
                            }
                        }
                        .fold(
                            ifLeft = { responseSender.sendMessage(buildSendMessage(target.chatId, preparedMessage, preparedNavigation)) },
                            ifRight = { Either.Right(Unit) }
                        )

                is ResponseTarget.Send ->
                    responseSender.sendMessage(buildSendMessage(target.chatId, preparedMessage, preparedNavigation))
            }
        }

    private fun <T : Message> localize(
        message: T,
        user: User,
        navigation: List<List<Button>>
    ): Either<EventProcessingError, Pair<PreparedMessage, List<List<Pair<Button, PreparedButton>>>>> {
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
            either {
                navigation
                    .filter { it.isNotEmpty() }
                    .map { row ->
                        row.map { button ->
                            navigationLocalizationService.localize(button, user.language)
                                .map { button to it }
                        }.bindAll()
                    }
            }.fold(
                ifLeft = {
                    logger.error { "Unable to localize navigation $message due to: $it" }
                    return Either.Left(LocalizationError)
                },
                ifRight = { it }
            )

        return Either.Right(localizedMessage to localizedNavigation)
    }

    private fun fetchResponseTarget(origin: Update): ResponseTarget {
        val chatId = origin.fetchUserId()

        if (!configurationProperties.interactiveMessageEditing) {
            return ResponseTarget.Send(chatId)
        }

        val pressedMessage = origin.callbackQuery()?.message() ?: return ResponseTarget.Send(chatId)

        if (pressedMessage.text() == null || pressedMessage.messageId() == null) {
            return ResponseTarget.Send(chatId)
        }

        return ResponseTarget.Edit(chatId = chatId, messageId = pressedMessage.messageId())
    }

    private fun buildSendMessage(
        chatId: String,
        message: PreparedMessage,
        navigation: List<List<Pair<Button, PreparedButton>>>,
        type: MessageType = MessageType.HTML,
    ): SendMessage {
        return SendMessage(chatId, message.text)
            .replyMarkup(navigation.toReplyKeyboard())
            .setParseMode(type)
            .linkPreviewOptions(LinkPreviewOptions().isDisabled(message.enablePreview.not()))
            .entities()
    }

    private fun buildEditMessage(
        chatId: String,
        messageId: Int,
        message: PreparedMessage,
        navigation: List<List<Pair<Button, PreparedButton>>>,
        type: MessageType = MessageType.HTML,
    ): EditMessageText {
        return EditMessageText(chatId, messageId, message.text)
            .replyMarkup(navigation.toInlineKeyboard())
            .setParseMode(type)
            .linkPreviewOptions(LinkPreviewOptions().isDisabled(message.enablePreview.not()))
    }

    private fun List<List<Pair<Button, PreparedButton>>>.toReplyKeyboard(): Keyboard {
        if (this.isEmpty()) {
            return ReplyKeyboardRemove()
        }

        return this.toInlineKeyboard()
    }

    private fun List<List<Pair<Button, PreparedButton>>>.toInlineKeyboard(): InlineKeyboardMarkup {
        val layout: List<List<InlineKeyboardButton>> = this
            .map { row -> row.map { it.toButton() } }

        return InlineKeyboardMarkup(*layout.map { it.toTypedArray() }.toTypedArray())
    }

    private fun Pair<Button, PreparedButton>.toButton(): InlineKeyboardButton {
        val (button, preparedButton) = this

        return InlineKeyboardButton(preparedButton.text)
            .callbackData(button.buildQualifiedName())
    }

    private sealed interface ResponseTarget {
        data class Send(val chatId: String) : ResponseTarget
        data class Edit(val chatId: String, val messageId: Int) : ResponseTarget
    }

    companion object {

        private val logger = KotlinLogging.logger { }

        private fun SendMessage.setParseMode(type: MessageType): SendMessage = when (type) {
            MessageType.PLAIN -> this
            MessageType.HTML -> this.parseMode(ParseMode.HTML)
        }

        private fun EditMessageText.setParseMode(type: MessageType): EditMessageText = when (type) {
            MessageType.PLAIN -> this
            MessageType.HTML -> this.parseMode(ParseMode.HTML)
        }
    }
}
