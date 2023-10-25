package org.grakovne.sideload.kindle.telegram.configuration

import arrow.core.Either
import arrow.core.sequence
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Update
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.ifTrue
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.LogLevel.WARN
import org.grakovne.sideload.kindle.events.internal.LoggingEvent
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.fetchLanguage
import org.grakovne.sideload.kindle.telegram.fetchUniqueIdentifier
import org.grakovne.sideload.kindle.telegram.fetchUserId
import org.grakovne.sideload.kindle.telegram.handlers.common.ButtonPressedEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.UnprocessedIncomingEventService
import org.grakovne.sideload.kindle.telegram.localization.EnumLocalizationService
import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageStatus
import org.grakovne.sideload.kindle.telegram.message.reference.service.MessageReferenceService
import org.grakovne.sideload.kindle.user.message.report.service.UserMessageReportService
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service

@Service
class MessageListenersConfiguration(
    private val bot: TelegramBot,
    private val incomingMessageEventListeners: List<ButtonPressedEventHandler<*>>,
    private val eventSender: EventSender,
    private val userService: UserService,
    private val enumLocalizationService: EnumLocalizationService,
    private val userMessageReportService: UserMessageReportService,
    private val unprocessedIncomingEventService: UnprocessedIncomingEventService,
    private val messageReferenceService: MessageReferenceService
) {

    @PostConstruct
    fun onCreate() = bot
        .setUpdatesListener { updates ->
            onMessageBatch(updates)
            UpdatesListener.CONFIRMED_UPDATES_ALL
        }

    private fun onMessageBatch(batch: List<Update>) {
        batch
            .forEach { update ->
                update
                    .also { logger.debug { "Received update $it. Processing" } }
                    .let { onMessage(it) }
            }
    }

    private fun onMessage(update: Update) = try {
        messageReferenceService
            .fetchMessage(update.fetchUniqueIdentifier())
            ?.let {
                if (it.status == MessageStatus.PROCESSED) {
                    logger.debug { "Got same message twice, message id: ${it.id}, skipping" }
                    return Either.Right(listOf(EventProcessingResult.SKIPPED))
                }
            }

        val user = userService.fetchOrCreateUser(
            userId = update.fetchUserId(),
            language = update.fetchLanguage()
        )

        logger.debug { "Processing incoming message ${update.updateId()} for user ${user.id}" }

        val incomingMessageEvent = ButtonPressedEvent(update, user)

        eventSender
            .sendEvent(incomingMessageEvent)
            .sequence()
            .tap {
                it
                    .processedByNothing()
                    .ifTrue {
                        unprocessedIncomingEventService
                            .handle(incomingMessageEvent)
                            .also {
                                logger.warn {
                                    "Unable to find acceptable listener for $incomingMessageEvent, sending default one response instead"
                                }
                            }
                    }
            }
            .also {
                bot
                    .also { logger.debug { "Updated set of available commands for ${user.id}" } }
            }
            .also {
                userMessageReportService
                    .createReportEntry(user.id, update.message()?.text())
                    .also { logger.debug { "Raw user message has been logged: ${it.text}" } }
            }
            .also { messageReferenceService.markAsProcessed(update.fetchUniqueIdentifier()) }

    } catch (ex: Exception) {
        logger.error { "Unable process incoming message. See Details: $ex" }
        eventSender.sendEvent(LoggingEvent(WARN, "Internal Exception. Message = ${ex.message}"))
    }


    private fun List<EventProcessingResult>.processedByNothing() =
        this.all { result -> result == EventProcessingResult.SKIPPED }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}