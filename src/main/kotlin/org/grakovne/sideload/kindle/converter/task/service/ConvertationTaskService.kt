package org.grakovne.sideload.kindle.converter.task.service

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.converter.ConvertationError
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converter.task.repository.ConvertationTaskRepository
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ConvertationTaskService(
    private val repository: ConvertationTaskRepository
) {

    fun fetchTasks(from: Instant, to: Instant) = repository.findByCreatedAtGreaterThanAndCreatedAtLessThan(from, to)

    fun updateTask(task: ConvertationTask) = repository
        .also { logger.debug { "Updating periodic task: $task" } }
        .save(task)
        .let { Either.Right(Unit) }

    fun submitTask(
        user: User,
        sourceFileUrl: String
    ): Either<ConvertationError, Unit> {
        val entity = ConvertationTask(
            id = UUID.randomUUID(),
            userId = user.id,
            sourceFileUrl = sourceFileUrl,
            createdAt = Instant.now(),
            status = ConvertationTaskStatus.ACTIVE,
            failReason = null
        )

        return repository
            .also { logger.debug { "Submitting to queue a new one convertation task: $entity" } }
            .save(entity)
            .let { Either.Right(Unit) }

    }

    fun fetchTasksForProcessing() =
        repository.findByStatusInAndCreatedAtLessThan(listOf(ConvertationTaskStatus.ACTIVE), Instant.now())

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}