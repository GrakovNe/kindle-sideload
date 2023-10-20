package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetFile
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.FileDownloadService
import org.grakovne.sideload.kindle.common.configuration.FileUploadProperties
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
import org.grakovne.sideload.kindle.user.configuration.domain.FileNotPresentedError
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

    override fun onEvent(event: IncomingMessageEvent): Either<UserConverterConfigurationError, EventProcessingResult> {
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
        when (code) {
            is ValidationError -> messageSender.sendResponse(
                origin = event.update,
                user = event.user,
                message = UserConfigurationValidationFailedMessage(code.code)
            )

            else -> messageSender.sendResponse(
                origin = event.update,
                user = event.user,
                message = UserConfigurationSubmissionFailedMessage
            )
        }
    }

    override fun processEvent(event: IncomingMessageEvent): Either<UserConverterConfigurationError, Unit> {
        val file = event
            .update
            .message()
            ?.document()
            ?: return Either.Left(FileNotPresentedError)

        if (file.fileSize() > properties.maxSize) {
            return Either.Left(FileIsTooLargeError)
        }

        val configurationFile = bot
            .execute(GetFile(file.fileId()))
            .file()
            .let { bot.getFullFilePath(it) }
            .let { fileDownloadService.download(it) }
            ?: return Either.Left(InternalError)

        return userConverterConfigurationService
            .updateConverterConfiguration(event.user, configurationFile)
            .map { }
            .also { userActivityStateService.dropCurrentState(event.user.id) }
    }

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}