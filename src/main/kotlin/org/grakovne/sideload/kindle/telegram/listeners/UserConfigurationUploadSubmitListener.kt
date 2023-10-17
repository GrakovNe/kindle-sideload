package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import arrow.core.flatMap
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetFile
import org.grakovne.sideload.kindle.common.FileDownloadService
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventProcessingResult.PROCESSED
import org.grakovne.sideload.kindle.events.core.EventProcessingResult.SKIPPED
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.localization.UserConfigurationFailedMessage
import org.grakovne.sideload.kindle.localization.UserConfigurationFailedReason
import org.grakovne.sideload.kindle.localization.UserConfigurationFailedReason.FILE_IS_TOO_LARGE
import org.grakovne.sideload.kindle.localization.UserConfigurationSubmittedMessage
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.messaging.SimpleMessageSender
import org.grakovne.sideload.kindle.telegram.state.domain.ActivityState
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationProperties
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.springframework.stereotype.Service

@Service
class UserConfigurationUploadSubmitListener(
    private val bot: TelegramBot,
    private val fileDownloadService: FileDownloadService,
    private val userActivityStateService: UserActivityStateService,
    private val userConverterConfigurationService: UserConverterConfigurationService,
    private val properties: UserConverterConfigurationProperties,
    private val messageSender: SimpleMessageSender,
) : IncomingMessageEventListener(), SilentEventListener {

    override fun onEvent(event: Event): Either<EventProcessingError<TelegramUpdateProcessingError>, EventProcessingResult> {
        if (event !is IncomingMessageEvent) {
            return Either.Right(SKIPPED)
        }

        return when (userActivityStateService.fetchCurrentState(event.user.id)) {
            ActivityState.UPLOADING_CONFIGURATION_REQUESTED -> processEvent(event).map { PROCESSED }
            else -> return Either.Right(SKIPPED)
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
                    message = UserConfigurationFailedMessage(FILE_IS_TOO_LARGE)
                )
                .mapLeft { EventProcessingError(TelegramUpdateProcessingError.RESPONSE_NOT_SENT) }
                .map { Unit }
        }

        val configurationFile = bot
            .execute(GetFile(file.fileId()))
            .file()
            .let { bot.getFullFilePath(it) }
            .let { fileDownloadService.download(it) }
            ?: return Either.Left(EventProcessingError(TelegramUpdateProcessingError.INTERNAL_ERROR))

        return userConverterConfigurationService
            .updateConverterConfiguration(event.user, configurationFile)
            .flatMap { userActivityStateService.dropCurrentState(event.user.id) }
            .mapLeft { EventProcessingError(TelegramUpdateProcessingError.INTERNAL_ERROR) }
            .tap {
                messageSender.sendResponse(
                    origin = event.update,
                    user = event.user,
                    message = UserConfigurationSubmittedMessage
                )
            }
    }

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)
}