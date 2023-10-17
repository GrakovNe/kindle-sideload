package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.GetFile
import org.grakovne.sideload.kindle.common.FileDownloadService
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.configuration.ConverterProperties
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.state.domain.ActivityState.UPLOADING_CONFIGURATION_REQUESTED
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class BookConversionListener(
    private val userActivityStateService: UserActivityStateService,
    private val converterProperties: ConverterProperties,
    private val fileDownloadService: FileDownloadService,
    private val bot: TelegramBot
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

        bot
            .execute(GetFile(file.fileId()))
            .file()
            .let { bot.getFullFilePath(it) }
            .let { fileDownloadService.download(it) }
            ?: return Either.Left(EventProcessingError(TelegramUpdateProcessingError.INTERNAL_ERROR))

        TODO("Not yet implemented")
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

}