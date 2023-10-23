package org.grakovne.sideload.kindle.telegram.listeners.screens.convertation

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetFile
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.BookIsTooLargeError
import org.grakovne.sideload.kindle.common.FileUploadFailedError
import org.grakovne.sideload.kindle.common.TaskQueueingError
import org.grakovne.sideload.kindle.common.configuration.FileUploadProperties
import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.domain.FileUploadFailedReason
import org.grakovne.sideload.kindle.telegram.listeners.InputRequiredEventListener
import org.grakovne.sideload.kindle.telegram.listeners.screens.main.RequestConvertationPromptButton
import org.grakovne.sideload.kindle.telegram.localization.domain.Button
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertationRequestedMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileUploadFailedMessage
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class BookConversionRequestListener(
    private val convertationTaskService: ConvertationTaskService,
    private val messageSender: NavigatedMessageSender,
    private val bot: TelegramBot,
    private val properties: FileUploadProperties,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : InputRequiredEventListener<FileUploadFailedError>(userActivityStateService, buttonService) {

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