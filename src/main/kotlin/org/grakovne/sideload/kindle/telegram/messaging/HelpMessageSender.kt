package org.grakovne.sideload.kindle.telegram.messaging

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.sequence
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.localization.EnumLocalizationService
import org.grakovne.sideload.kindle.localization.MessageLocalizationService
import org.grakovne.sideload.kindle.telegram.domain.CommandType
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.sideload.kindle.telegram.domain.error.UnknownError
import org.grakovne.sideload.kindle.telegram.navigation.HelpMessage
import org.grakovne.sideload.kindle.telegram.navigation.HelpMessageItem
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service

@Service
class HelpMessageSender(
    bot: TelegramBot,
    private val localizationService: MessageLocalizationService,
    private val enumLocalizationService: EnumLocalizationService
) : MessageSender(bot) {

    fun sendResponse(
        origin: Update,
        user: User,
        helpMessage: List<Help>
    ): Either<EventProcessingError, Unit> {
        val targetLanguage = user.language

        return helpMessage
            .map {
                HelpMessageItem(
                    key = buildCommandUsage(it),
                    description = enumLocalizationService.localize(it.description, targetLanguage)
                )
            }
            .map { localizationService.localize(it, targetLanguage) }
            .sequence()
            .map { it.joinToString(separator = "\n", transform = PreparedMessage::text) }
            .map { HelpMessage(it) }
            .flatMap { localizationService.localize(it, targetLanguage) }
            .mapLeft { UnknownError }
            .flatMap { sendRawMessage(origin, it) }
    }

    private fun buildCommandUsage(it: Help): String = it.key
}

data class Help(
    val key: String,
    val description: CommandType
)