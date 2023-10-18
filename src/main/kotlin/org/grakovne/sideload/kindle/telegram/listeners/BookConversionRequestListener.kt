package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.GetFile
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.FileUploadFailedReason
import org.grakovne.sideload.kindle.common.configuration.FileUploadProperties
import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.localization.FileConvertationRequestedMessage
import org.grakovne.sideload.kindle.localization.UserConfigurationFailedMessage
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.configuration.ConverterProperties
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.messaging.SimpleMessageSender
import org.grakovne.sideload.kindle.telegram.state.domain.ActivityState.UPLOADING_CONFIGURATION_REQUESTED
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class BookConversionRequestListener(
    private val converterProperties: ConverterProperties,
    private val convertationTaskService: ConvertationTaskService,
    private val userActivityStateService: UserActivityStateService,
    private val messageSender: SimpleMessageSender,
    private val bot: TelegramBot,
    private val properties: FileUploadProperties,
) : IncomingMessageEventListener(), SilentEventListener {

    override fun onEvent(event: Event): Either<EventProcessingError<TelegramUpdateProcessingError>, EventProcessingResult> {
        if (event !is IncomingMessageEvent) {
            return Either.Right(EventProcessingResult.SKIPPED)
        }

        return when {
            userActivityStateService.fetchCurrentState(event.user.id) == UPLOADING_CONFIGURATION_REQUESTED ->
                return Either.Right(EventProcessingResult.SKIPPED)

            receivedSourceFile(event.update) ->
                processEvent(event).map { EventProcessingResult.PROCESSED }

            else ->
                return Either.Right(EventProcessingResult.SKIPPED)
        }
    }

    override fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError<TelegramUpdateProcessingError>, Unit> {
        val file = event
            .update
            .message()
            ?.document()
            ?: return Either.Right(Unit)

        if (file.fileSize() > properties.maxSize) {
            return messageSender
                .sendResponse(
                    origin = event.update,
                    user = event.user,
                    message = UserConfigurationFailedMessage(FileUploadFailedReason.FILE_IS_TOO_LARGE)
                )
                .mapLeft { EventProcessingError(TelegramUpdateProcessingError.RESPONSE_NOT_SENT) }
                .map { Unit }
        }

        val sourceUrl = bot
            .execute(GetFile(file.fileId()))
            .file()
            .let { bot.getFullFilePath(it) }

        return convertationTaskService
            .submitTask(event.user, sourceFileUrl = sourceUrl)
            .mapLeft { EventProcessingError(TelegramUpdateProcessingError.INTERNAL_ERROR) }
            .tap {
                messageSender.sendResponse(
                    origin = event.update,
                    user = event.user,
                    message = FileConvertationRequestedMessage
                )
            }
    }

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)

    private fun receivedSourceFile(update: Update): Boolean {
        val file = update
            .message()
            ?.document()
            ?: return false

        return converterProperties
            .sourceFileExtensions
            .any { file.fileName().endsWith(it) }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}