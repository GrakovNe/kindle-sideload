package org.grakovne.sideload.kindle.telegram.configuration

import arrow.core.Either
import arrow.core.sequence
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.BotCommand
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SetMyCommands
import jakarta.annotation.PostConstruct
import org.grakovne.sideload.kindle.common.ifTrue
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.LogLevel.WARN
import org.grakovne.sideload.kindle.events.internal.LoggingEvent
import org.grakovne.sideload.kindle.localization.EnumLocalizationService
import org.grakovne.sideload.kindle.localization.Language
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.listeners.IncomingMessageEventListener
import org.grakovne.sideload.kindle.telegram.listeners.UnprocessedIncomingEventHandler
import org.grakovne.sideload.kindle.telegram.messaging.provideLanguage
import org.grakovne.sideload.kindle.user.UserMessageReportService
import org.grakovne.sideload.kindle.user.UserReferenceService
import org.grakovne.sideload.kindle.user.domain.UserReferenceSource
import org.springframework.stereotype.Service

@Service
class MessageListenersConfiguration(
    private val bot: TelegramBot,
    private val incomingMessageEventListeners: List<IncomingMessageEventListener>,
    private val eventSender: EventSender,
    private val userReferenceService: UserReferenceService,
    private val enumLocalizationService: EnumLocalizationService,
    private val userMessageReportService: UserMessageReportService,
    private val unprocessedIncomingEventHandler: UnprocessedIncomingEventHandler
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

        val incomingMessageEvent = IncomingMessageEvent(update, user)

        eventSender
            .sendEvent(incomingMessageEvent)
            .sequence()
            .tap { it.processedByNothing().ifTrue { unprocessedIncomingEventHandler.handle(incomingMessageEvent) } }
            .also { bot.execute(SetMyCommands(*messageListenersDescriptions(user.provideLanguage()))) }
            .also { userMessageReportService.createReportEntry(user.id, update.message()?.text()) }

    } catch (ex: Exception) {
        eventSender.sendEvent(LoggingEvent(WARN, "Internal Exception. Message = ${ex.message}"))
        Either.Left(TelegramUpdateProcessingError.INTERNAL_ERROR)
    }

    private fun Update.hasSender() = this.message()?.chat()?.id() != null
    private fun Update.hasMessage() = this.message()?.text() != null

    private fun messageListenersDescriptions(targetLanguage: Language) = incomingMessageEventListeners
        .map {
            BotCommand(
                it.getDescription().key,
                enumLocalizationService.localize(it.getDescription().type, targetLanguage)
            )
        }
        .toTypedArray()

    private fun List<EventProcessingResult>.processedByNothing() =
        this.all { result -> result == EventProcessingResult.SKIPPED }
}