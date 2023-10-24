package org.grakovne.sideload.kindle.environment

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventListener
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.internal.UserEnvironmentUnnecessaryEvent
import org.springframework.stereotype.Service

//@Service
class UserEnvironmentUnnecessaryListener(
    private val environmentService: UserEnvironmentService
) : EventListener<UserEnvironmentUnnecessaryEvent, EnvironmentError>() {
    override fun acceptableEvents() = listOf(EventType.ENVIRONMENT_UNNECESSARY)

    override fun onEvent(event: UserEnvironmentUnnecessaryEvent): Either<EnvironmentError, EventProcessingResult> {
        logger.info { "Processing $event with ${this.javaClass.simpleName}" }

        return environmentService
            .terminateEnvironment(event.environmentId ?: return Either.Right(EventProcessingResult.SKIPPED))
            .map { EventProcessingResult.PROCESSED }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}