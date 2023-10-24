package org.grakovne.sideload.kindle.transferring.email.service

import arrow.core.Either
import org.grakovne.sideload.kindle.converter.ConvertationError
import org.grakovne.sideload.kindle.transferring.email.domain.TransferEmailTask
import org.grakovne.sideload.kindle.transferring.email.domain.TransferEmailTaskStatus
import org.grakovne.sideload.kindle.transferring.email.repository.TransferEmailTaskRepository
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
        user: User,
        environmentId: String
    ): Either<ConvertationError, Unit> {
        val entity = TransferEmailTask(
            id = UUID.randomUUID(),
            userId = user.id,
            environmentId = environmentId,
            createdAt = Instant.now(),
            status = TransferEmailTaskStatus.ACTIVE,
            failReason = null
        )

        return repository
            .save(entity)
            .let { Either.Right(Unit) }

    }

    fun fetchTasksForProcessing() =
        repository.findByStatusInAndCreatedAtLessThan(listOf(TransferEmailTaskStatus.ACTIVE), Instant.now())
}