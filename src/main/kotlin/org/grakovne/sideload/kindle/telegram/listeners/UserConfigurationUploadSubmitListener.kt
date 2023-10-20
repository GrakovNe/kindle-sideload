package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetFile
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.FileDownloadService
import org.grakovne.sideload.kindle.common.configuration.FileUploadProperties
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventProcessingResult.PROCESSED
import org.grakovne.sideload.kindle.events.core.EventProcessingResult.SKIPPED
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.localization.UserConfigurationSubmissionFailedMessage
import org.grakovne.sideload.kindle.localization.UserConfigurationSubmittedMessage
import org.grakovne.sideload.kindle.localization.UserConfigurationValidationFailedMessage
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.messaging.SimpleMessageSender
import org.grakovne.sideload.kindle.telegram.state.domain.ActivityState
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.grakovne.sideload.kindle.user.configuration.domain.FileIsTooLargeError
import org.grakovne.sideload.kindle.user.configuration.domain.InternalError
import org.grakovne.sideload.kindle.user.configuration.domain.UserConverterConfigurationError
import org.grakovne.sideload.kindle.user.configuration.domain.ValidationError
import org.springframework.stereotype.Service

@Service
class UserConfigurationUploadSubmitListener(
    private val bot: TelegramBot,
    private val fileDownloadService: FileDownloadService,
    private val userActivityStateService: UserActivityStateService,
    private val userConverterConfigurationService: UserConverterConfigurationService,
    private val properties: FileUploadProperties,
    private val messageSender: SimpleMessageSender,
    private val userConfigurationUploadRequestListener: UserConfigurationUploadRequestListener
) : IncomingMessageEventListener<UserConverterConfigurationError>(), SilentEventListener {

    override fun onEvent(event: IncomingMessageEvent): Either<EventProcessingError<UserConverterConfigurationError>, EventProcessingResult> {
        if (event.acceptForListener(userConfigurationUploadRequestListener.getDescription())) {
            return Either.Right(SKIPPED)
        }

        return when (userActivityStateService.fetchCurrentState(event.user.id)) {
            ActivityState.UPLOADING_CONFIGURATION_REQUESTED -> processEvent(event).map { PROCESSED }
            else -> return Either.Right(SKIPPED)
        }
    }

    override fun sendSuccessfulResponse(event: IncomingMessageEvent) {
        messageSender.sendResponse(
            origin = event.update,
            user = event.user,
            message = UserConfigurationSubmittedMessage
        )
    }

    override fun sendFailureResponse(event: IncomingMessageEvent, code: UserConverterConfigurationError) {
        handleConfigurationUpdateError(code, event)
    }

    override fun processEvent(event: IncomingMessageEvent): Either<EventProcessingError<UserConverterConfigurationError>, Unit> {
        val file = event
            .update
            .message()
            ?.document()
            ?: return Either.Right(Unit)

        if (file.fileSize() > properties.maxSize) {
            return Either.Left(EventProcessingError(FileIsTooLargeError))
        }

        val configurationFile = bot
            .execute(GetFile(file.fileId()))
            .file()
            .let { bot.getFullFilePath(it) }
            .let { fileDownloadService.download(it) }
            ?: return Either.Left(EventProcessingError(InternalError))

        return userConverterConfigurationService
            .updateConverterConfiguration(event.user, configurationFile)
            .mapLeft { EventProcessingError(it) }
            .map { Unit }
            .also { userActivityStateService.dropCurrentState(event.user.id) }
    }

    private fun handleConfigurationUpdateError(
        error: UserConverterConfigurationError,
        event: IncomingMessageEvent
    ): Either.Left<EventProcessingError<UserConverterConfigurationError>> {
        when (error) {
            is ValidationError -> messageSender.sendResponse(
                origin = event.update,
                user = event.user,
                message = UserConfigurationValidationFailedMessage(error.code)
            )

            else -> messageSender.sendResponse(
                origin = event.update,
                user = event.user,
                message = UserConfigurationSubmissionFailedMessage
            )
        }

        return Either.Left(EventProcessingError(InternalError))
    }

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}