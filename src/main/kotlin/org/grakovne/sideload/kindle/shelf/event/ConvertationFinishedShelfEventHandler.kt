package org.grakovne.sideload.kindle.shelf.event

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.ConvertationFinished
import org.grakovne.sideload.kindle.events.core.EventHandler
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import org.grakovne.sideload.kindle.shelf.common.ShelfItemError
import org.grakovne.sideload.kindle.shelf.common.UnableAttachItemError
import org.grakovne.sideload.kindle.shelf.service.ShelfItemService
import org.grakovne.sideload.kindle.shelf.service.ShelfService
import org.springframework.stereotype.Service

@Service
class ConvertationFinishedShelfEventHandler(
    private val shelfService: ShelfService,
    private val shelfItemService: ShelfItemService
) : EventHandler<ConvertationFinishedEvent, EventProcessingError>() {

    override fun acceptableEvents(): List<EventType> = listOf(ConvertationFinished)

    override fun onEvent(event: ConvertationFinishedEvent): Either<EventProcessingError, EventProcessingResult> {
        if (event.status != ConvertationFinishedStatus.SUCCESS) {
            return Either.Right(EventProcessingResult.SKIPPED)
        }

        val result = event
            .environmentId
            ?.let { environmentId ->
                shelfService
                    .fetchOrCreateShelf(event.userId)
                    .let {
                        shelfItemService.attachToShelf(
                            shelfId = it.id,
                            environmentId = environmentId
                        )
                    }
            }
            ?: Either.Left(ShelfItemError.ITEM_NOT_CREATED)

        return when (result) {
            is Either.Left -> {
                logger.error { "Unable to attach the convertation result $event to the shelf due to: $result" }
                Either.Left(UnableAttachItemError)
            }

            is Either.Right -> Either.Right(EventProcessingResult.PROCESSED)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}

