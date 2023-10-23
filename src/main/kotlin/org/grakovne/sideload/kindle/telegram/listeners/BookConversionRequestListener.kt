package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.GetFile
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.BookIsTooLargeError
import org.grakovne.sideload.kindle.common.FileUploadFailedError
import org.grakovne.sideload.kindle.common.TaskQueueingError
import org.grakovne.sideload.kindle.common.configuration.FileUploadProperties
import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.configuration.ConverterProperties
import org.grakovne.sideload.kindle.telegram.domain.FileUploadFailedReason
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertationRequestedMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileUploadFailedMessage
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class BookConversionRequestListener(
    private val converterProperties: ConverterProperties,
    private val convertationTaskService: ConvertationTaskService,
    private val buttonService: ButtonService,
    private val userActivityStateService: UserActivityStateService,
    private val messageSender: NavigatedMessageSender,
    private val bot: TelegramBot,
    private val properties: FileUploadProperties,
) : ButtonPressedEventListener<FileUploadFailedError>(buttonService, userActivityStateService), SilentEventListener {

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender.sendResponse(
            origin = event.update,
            user = event.user,
            message = FileConvertationRequestedMessage
        )
    }

    override fun sendFailureResponse(event: ButtonPressedEvent, code: FileUploadFailedError) {
        messageSender
            .sendResponse(
                origin = event.update,
                user = event.user,
                message = FileUploadFailedMessage(FileUploadFailedReason.FILE_IS_TOO_LARGE)
            )
    }

    override fun onEvent(event: ButtonPressedEvent): Either<FileUploadFailedError, EventProcessingResult> {
        return when {
//            userActivityStateService.fetchCurrentState(event.user.id) == UPLOADING_CONFIGURATION_REQUESTED ->
//                return Either.Right(EventProcessingResult.SKIPPED)

            receivedSourceFile(event.update) ->
                processEvent(event).map { EventProcessingResult.PROCESSED }

            else -> return Either.Right(EventProcessingResult.SKIPPED)
        }
    }

    override fun processEvent(event: ButtonPressedEvent): Either<FileUploadFailedError, Unit> {
        val file = event
            .update
            .message()
            ?.document()
            ?: return Either.Right(Unit)

        if (file.fileSize() > properties.maxSize) {
            return Either.Left(BookIsTooLargeError)
        }

        val sourceUrl = bot
            .execute(GetFile(file.fileId()))
            .file()
            .let { bot.getFullFilePath(it) }

        return convertationTaskService
            .submitTask(event.user, sourceFileUrl = sourceUrl)
            .mapLeft { TaskQueueingError }
    }

    private fun receivedSourceFile(update: Update): Boolean {
        val file = update
            .message()
            ?.document()
            ?: return false

        return converterProperties
            .sourceFileExtensions
            .any { file.fileName().equals(it) }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}