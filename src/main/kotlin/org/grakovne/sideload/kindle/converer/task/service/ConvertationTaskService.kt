package org.grakovne.sideload.kindle.converer.task.service

import arrow.core.Either
import org.grakovne.sideload.kindle.converer.ConvertationError
import org.grakovne.sideload.kindle.converer.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converer.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converer.task.repository.ConvertationTaskRepository
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class ConvertationTaskService(
    private val repository: ConvertationTaskRepository
) {

    fun updateTask(task: ConvertationTask) = repository
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
            .save(entity)
            .let { Either.Right(Unit) }

    }

    fun fetchTasksForProcessing() =
        repository.findByStatusInAndCreatedAtLessThan(listOf(ConvertationTaskStatus.ACTIVE), Instant.now())
}