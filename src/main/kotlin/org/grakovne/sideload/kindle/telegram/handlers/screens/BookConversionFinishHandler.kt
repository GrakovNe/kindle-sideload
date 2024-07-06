package org.grakovne.sideload.kindle.telegram.handlers.screens

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendDocument
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.common.parallelMap
import org.grakovne.sideload.kindle.converter.ConvertationError
import org.grakovne.sideload.kindle.converter.FileNotSupported
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import org.grakovne.sideload.kindle.shelf.service.ShelfService
import org.grakovne.sideload.kindle.telegram.domain.error.UnknownError
import org.grakovne.sideload.kindle.telegram.handlers.common.ReplyingEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.convertation.SendConvertedToEmailButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionErrorMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionFailedUnsupportedMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionSuccessAutomaticStkMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionSuccessEmptyOutputMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionSuccessMessage
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service

@Service
class BookConversionFinishHandler(
    private val bot: TelegramBot,
    private val messageSender: MessageWithNavigationSender,
    private val userService: UserService,
    private val userPreferencesService: UserPreferencesService,
    private val shelfService: ShelfService
) : ReplyingEventHandler<ConvertationFinishedEvent, EventProcessingError>() {

    override fun acceptableEvents(): List<EventType> = listOf(EventType.CONVERTATION_FINISHED)

    override fun sendSuccessfulResponse(event: ConvertationFinishedEvent) {
        val user = userService.fetchUser(event.userId)

        runBlocking {
            event
                .output
                .map { SendDocument(event.userId, it) }
                .parallelMap { bot.execute(it) }
        }
            .also { sendSuccessMessage(event, user) }
    }

    private fun sendSuccessMessage(event: ConvertationFinishedEvent, user: User) {
        when {
            userPreferencesService.fetchPreferences(user.id).automaticStk -> {
                messageSender
                    .sendResponse(
                        chatId = user.id,
                        user = user,
                        message = FileConvertarionSuccessAutomaticStkMessage(event.log),
                        navigation = emptyList()
                    )
            }

            event.output.isEmpty() -> {
                messageSender
                    .sendResponse(
                        chatId = user.id,
                        user = user,
                        message = FileConvertarionSuccessEmptyOutputMessage(event.log),
                        navigation = listOf(
                            listOf(MainScreenButton),
                        )
                    )
            }

            else -> {
                messageSender
                    .sendResponse(
                        chatId = user.id,
                        user = user,
                        message = FileConvertarionSuccessMessage(shelfService.fetchShelfLink(event.userId)),
                        navigation = listOf(
                            listOf(SendConvertedToEmailButton(event.environmentId)),
                            listOf(MainScreenButton),
                        )
                    )
            }
        }
    }

    override fun sendFailureResponse(event: ConvertationFinishedEvent, code: EventProcessingError) {
        val user = userService.fetchUser(event.userId)

        messageSender
            .sendResponse(
                chatId = user.id,
                user = user,
                message = event.failureReason.toMessage(event.log),
                navigation = listOf(listOf(MainScreenButton))
            )
    }

    override fun onEvent(event: ConvertationFinishedEvent): Either<EventProcessingError, EventProcessingResult> =
        when (event.status) {
            ConvertationFinishedStatus.SUCCESS -> Either.Right(EventProcessingResult.PROCESSED)
            ConvertationFinishedStatus.FAILED -> Either.Left(UnknownError)
        }

    companion object {
        private fun ConvertationError?.toMessage(log: String): Message {
            return when (this) {
                FileNotSupported -> FileConvertarionFailedUnsupportedMessage
                else -> FileConvertarionErrorMessage(log)
            }
        }

        private val logger = KotlinLogging.logger { }
    }

}