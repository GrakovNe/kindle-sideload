package org.grakovne.sideload.kindle.telegram.handlers.screens.convertation

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetFile
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.BookIsTooLargeError
import org.grakovne.sideload.kindle.common.FileUploadFailedError
import org.grakovne.sideload.kindle.common.TaskQueueingError
import org.grakovne.sideload.kindle.common.configuration.FileUploadProperties
import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.domain.FileUploadFailedReason
import org.grakovne.sideload.kindle.telegram.handlers.common.InputRequiredEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestConvertationPromptButton
import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigation
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertationRequestedMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileUploadFailedMessage
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class BookConversionRequestHandler(
    private val convertationTaskService: ConvertationTaskService,
    private val messageSender: MessageWithNavigation,
    private val bot: TelegramBot,
    private val properties: FileUploadProperties,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : InputRequiredEventHandler<FileUploadFailedError>(userActivityStateService, buttonService) {

    override fun getRequiredButton(): List<Button> = listOf(RequestConvertationPromptButton)

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
        return event
            .update
            .message()
            ?.document()
            ?.let { super.onEvent(event) }
            ?: Either.Right(EventProcessingResult.SKIPPED)
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

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}