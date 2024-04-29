package org.grakovne.sideload.kindle.stk.email.task.service

import arrow.core.Either
import org.grakovne.sideload.kindle.converter.ConvertationError
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTask
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTaskStatus
import org.grakovne.sideload.kindle.stk.email.task.repository.TransferEmailTaskRepository
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class TransferEmailTaskService(
    private val repository: TransferEmailTaskRepository
) {

    fun updateTask(task: TransferEmailTask) = repository
        .save(task)
        .let { Either.Right(Unit) }

    fun submitTask(
        userId: String,
        environmentId: String
    ): Either<ConvertationError, Unit> {
        val entity = TransferEmailTask(
            id = UUID.randomUUID(),
            userId = userId,
            environmentId = environmentId,
            createdAt = Instant.now(),
            status = TransferEmailTaskStatus.ACTIVE,
            failReason = null
        )

        return repository
            .save(entity)
            .let { Either.Right(Unit) }

    }

    fun fetchLatestForProcessing(): TransferEmailTask? =
        repository
            .findByStatusInAndCreatedAtLessThan(listOf(TransferEmailTaskStatus.ACTIVE), Instant.now())
            .firstOrNull()
}