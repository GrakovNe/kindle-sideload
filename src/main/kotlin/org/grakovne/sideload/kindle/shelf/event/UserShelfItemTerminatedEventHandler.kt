package org.grakovne.sideload.kindle.shelf.event

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EnvironmentUnnecessary
import org.grakovne.sideload.kindle.events.core.EventHandler
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.internal.UserEnvironmentUnnecessaryEvent
import org.grakovne.sideload.kindle.shelf.common.ShelfProcessingError
import org.grakovne.sideload.kindle.shelf.common.UnableTerminateItemError
import org.grakovne.sideload.kindle.shelf.service.ShelfItemService
import org.springframework.stereotype.Service

@Service
class UserShelfItemTerminatedEventHandler(
    private val shelfItemService: ShelfItemService
) : EventHandler<UserEnvironmentUnnecessaryEvent, EventProcessingError>() {
    override fun acceptableEvents() = listOf(EnvironmentUnnecessary)

    override fun onEvent(event: UserEnvironmentUnnecessaryEvent): Either<ShelfProcessingError, EventProcessingResult> {
        logger.info { "Processing $event with ${this.javaClass.simpleName}" }

        return event
            .environmentId
            ?.let {
                shelfItemService
                    .terminateItem(it)
                    .mapLeft { UnableTerminateItemError }
                    .map { EventProcessingResult.PROCESSED }
            }
            ?: Either.Right(EventProcessingResult.SKIPPED)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}