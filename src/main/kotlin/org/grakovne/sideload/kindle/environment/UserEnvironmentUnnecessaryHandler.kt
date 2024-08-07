package org.grakovne.sideload.kindle.environment

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.events.core.EventHandler
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.internal.UserEnvironmentUnnecessaryEvent
import org.springframework.stereotype.Service

@Service
class UserEnvironmentUnnecessaryHandler(
    private val environmentService: UserEnvironmentService
) : EventHandler<UserEnvironmentUnnecessaryEvent, EnvironmentError>() {
    override fun acceptableEvents() = listOf(EnvironmentUnnecessary)

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