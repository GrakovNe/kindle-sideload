package org.grakovne.sideload.kindle.telegram.listeners

import arrow.core.Either
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventProcessingResult.*
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.telegram.TelegramUpdateProcessingError
import org.grakovne.sideload.kindle.telegram.domain.IncomingMessageEvent
import org.grakovne.sideload.kindle.telegram.state.domain.ActivityState
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class UserConfigurationUploadSubmitListener(
    private val userActivityStateService: UserActivityStateService
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
        println(event)

        return Either.Right(Unit)
    }

    override fun acceptableEvents(): List<EventType> = listOf(EventType.INCOMING_MESSAGE)
}