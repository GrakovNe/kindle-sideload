package org.grakovne.sideload.kindle.environment.periodic

import mu.KotlinLogging
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.environment.configuration.EnvironmentProperties
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.UserEnvironmentUnnecessaryEvent
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant


@Service
class EnvironmentTerminationPeriodicTask(
    private val eventSender: EventSender,
    private val environmentProperties: EnvironmentProperties,
    private val userEnvironmentService: UserEnvironmentService
) {

    @Scheduled(fixedDelay = 5000)
    fun terminateOutdatedEnvironments() {
        val terminateCreatedBefore = Instant.now().minusSeconds(environmentProperties.ttlInSeconds)

        userEnvironmentService
            .provideTemporaryEnvironmentsFolder()
            .listFiles()
            ?.filter { it.isDirectory }
            ?.filter {
                val attributes = Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)
                attributes.creationTime().toInstant().isBefore(terminateCreatedBefore)
            }
            ?.forEach {
                logger.debug { "Terminating outdated environment: ${it.name}" }

                eventSender.sendEvent(
                    UserEnvironmentUnnecessaryEvent(
                        environmentId = it.name
                    )
                )
            }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}