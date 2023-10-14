package org.grakovne.sideload.kindle.telegram.command

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.BotCommand
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SetMyCommands
import jakarta.annotation.PostConstruct
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.LogLevel.WARN
import org.grakovne.sideload.kindle.events.internal.LoggingEvent
import org.grakovne.sideload.kindle.localization.EnumLocalizationService
import org.grakovne.sideload.kindle.localization.Language
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.messaging.provideLanguage
import org.grakovne.sideload.kindle.user.UserMessageReportService
import org.grakovne.sideload.kindle.user.UserReferenceService
import org.grakovne.sideload.kindle.user.domain.UserReferenceSource
import org.springframework.stereotype.Service

@Service
class TelegramOnMessageConfiguration(
    private val bot: TelegramBot,
    private val unknownCommandProcessor: UnknownMessageCommandProcessingService,
    private val commands: List<TelegramOnMessageCommand>,
    private val eventSender: EventSender,
    private val userReferenceService: UserReferenceService,
    private val enumLocalizationService: EnumLocalizationService,
    private val userMessageReportService: UserMessageReportService
) {

    @PostConstruct
    fun onCreate() = bot
        .setUpdatesListener { updates ->
            onMessageBatch(updates)
            UpdatesListener.CONFIRMED_UPDATES_ALL
        }

    private fun onMessageBatch(batch: List<Update>) =
        batch
            .filter { update -> update.hasSender() }
            .filter { update -> update.hasMessage() }
            .forEach { update -> onMessage(update) }

    private fun onMessage(update: Update) = try {
        val user = userReferenceService.fetchUser(
            userId = update.message().chat().id().toString(),
            source = UserReferenceSource.TELEGRAM,
            language = update.message()?.from()?.languageCode() ?: "en"
        )

        update
            .findCommand()
            .accept(update, user)
            .tap { bot.execute(SetMyCommands(*commandsDescription(user.provideLanguage()))) }
            .tap { userMessageReportService.createReportEntry(user.id, update.message()?.text()) }
    } catch (ex: Exception) {
        eventSender.sendEvent(LoggingEvent(WARN, "Internal Exception. Message = ${ex.message}"))
        Either.Left(TelegramUpdateProcessingError.INTERNAL_ERROR)
    }

    private fun Update.findCommand() =
        commands
            .find { command -> command.isAcceptable(this) }
            ?: unknownCommandProcessor.findCommand(this)

    private fun Update.hasSender() = this.message()?.chat()?.id() != null
    private fun Update.hasMessage() = this.message()?.text() != null

    private fun commandsDescription(targetLanguage: Language) = commands
        .map {
            BotCommand(
                it.getKey(),
                enumLocalizationService.localize(it.getType(), targetLanguage)
            )
        }
        .toTypedArray()
}