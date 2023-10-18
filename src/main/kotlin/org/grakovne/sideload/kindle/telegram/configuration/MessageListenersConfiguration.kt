package org.grakovne.sideload.kindle.telegram.configuration

import arrow.core.Either
import arrow.core.sequence
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.BotCommand
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SetMyCommands
import jakarta.annotation.PostConstruct
import org.grakovne.sideload.kindle.common.Language
import org.grakovne.sideload.kindle.common.ifTrue
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.LogLevel.WARN
import org.grakovne.sideload.kindle.events.internal.LoggingEvent
import org.grakovne.sideload.kindle.localization.EnumLocalizationService
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.listeners.IncomingMessageEventListener
import org.grakovne.sideload.kindle.telegram.listeners.UnprocessedIncomingEventHandler
import org.grakovne.sideload.kindle.user.message.report.service.UserMessageReportService
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service

@Service
class MessageListenersConfiguration(
    private val bot: TelegramBot,
    private val incomingMessageEventListeners: List<IncomingMessageEventListener>,
    private val eventSender: EventSender,
    private val userService: UserService,
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

    private fun onMessageBatch(batch: List<Update>) {
        batch
            .forEach { update -> onMessage(update) }
    }

    private fun onMessage(update: Update) = try {
        val user = userService.fetchOrCreateUser(
            userId = update.message().chat().id().toString(),
            language = update.message()?.from()?.languageCode() ?: "en"
        )

        val incomingMessageEvent = IncomingMessageEvent(update, user)

        eventSender
            .sendEvent(incomingMessageEvent)
            .sequence()
            .tap { it.processedByNothing().ifTrue { unprocessedIncomingEventHandler.handle(incomingMessageEvent) } }
            .also { bot.execute(SetMyCommands(*messageListenersDescriptions(user.language))) }
            .also { userMessageReportService.createReportEntry(user.id, update.message()?.text()) }

    } catch (ex: Exception) {
        eventSender.sendEvent(LoggingEvent(WARN, "Internal Exception. Message = ${ex.message}"))
        Either.Left(TelegramUpdateProcessingError.INTERNAL_ERROR)
    }

    private fun messageListenersDescriptions(targetLanguage: Language?) = incomingMessageEventListeners
        .mapNotNull { it.getDescription() }
        .map {
            BotCommand(
                it.key,
                enumLocalizationService.localize(it.type, targetLanguage)
            )
        }
        .toTypedArray()

    private fun List<EventProcessingResult>.processedByNothing() =
        this.all { result -> result == EventProcessingResult.SKIPPED }
}