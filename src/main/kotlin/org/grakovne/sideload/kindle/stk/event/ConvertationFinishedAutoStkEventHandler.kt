package org.grakovne.sideload.kindle.stk.event

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventHandler
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import org.grakovne.sideload.kindle.stk.email.task.domain.InternalError
import org.grakovne.sideload.kindle.stk.email.task.service.TransferEmailTaskService
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.springframework.stereotype.Service

@Service
class ConvertationFinishedAutoStkEventHandler(
    private val userPreferencesService: UserPreferencesService,
    private val transferEmailTaskService: TransferEmailTaskService
) : EventHandler<ConvertationFinishedEvent, EventProcessingError>() {

    override fun acceptableEvents(): List<EventType> = listOf(EventType.CONVERTATION_FINISHED)

    override fun onEvent(event: ConvertationFinishedEvent): Either<EventProcessingError, EventProcessingResult> {
        if (event.status == ConvertationFinishedStatus.FAILED) {
            logger.trace { "Got ConvertationFinishedEvent with failed status, nothing to STK" }
            return Either.Right(EventProcessingResult.SKIPPED)
        }

        return when (userPreferencesService.fetchPreferences(event.userId).automaticStk) {
            true -> {
                val environmentId = event.environmentId ?: return Either
                    .Right(EventProcessingResult.SKIPPED)
                    .also { logger.error { "Got ConvertationFinishedEvent but Auto STK is enabled, but environment id is null" } }

                transferEmailTaskService
                    .submitTask(event.userId, environmentId = environmentId)
                    .mapLeft { InternalError }
                    .map { EventProcessingResult.PROCESSED }

            }

            false -> {
                logger.trace { "Got ConvertationFinishedEvent but Auto STK is disabled" }
                Either.Right(EventProcessingResult.SKIPPED)
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
