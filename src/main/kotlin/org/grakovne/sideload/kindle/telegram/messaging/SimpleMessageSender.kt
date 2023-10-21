package org.grakovne.sideload.kindle.telegram.messaging

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.sequence
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.error.LocalizationError
import org.grakovne.sideload.kindle.telegram.localization.Message
import org.grakovne.sideload.kindle.telegram.localization.MessageLocalizationService
import org.grakovne.sideload.kindle.telegram.localization.NavigationItem
import org.grakovne.sideload.kindle.telegram.localization.NavigationLocalizationService
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service

@Service
class SimpleMessageSender(
    bot: TelegramBot,
    private val navigationLocalizationService: NavigationLocalizationService,
    private val messageLocalizationService: MessageLocalizationService
) : MessageSender(bot) {

    fun <T : Message> sendResponse(
        chatId: String,
        user: User,
        message: T,
        navigation: List<NavigationItem> = emptyList()
    ): Either<EventProcessingError, Unit> {
        val localizedMessage = messageLocalizationService.localize(message, user.language)
        val localizedNavigation = navigation.map { navigationLocalizationService.localize(it, user.language) }.sequence()

        return localizedMessage
            .flatMap { preparedMessage ->
                localizedNavigation.flatMap { preparedNavigation ->
                    sendRawMessage(
                        chatId,
                        preparedMessage,
                        preparedNavigation
                    )
                }
            }
            .mapLeft { LocalizationError }

    }

    fun <T : Message> sendResponse(
        origin: Update,
        user: User,
        message: T,
        navigation: List<NavigationItem> = emptyList()
    ) = sendResponse(origin.message().chat().id().toString(), user, message, navigation)
}