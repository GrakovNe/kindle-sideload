package org.grakovne.sideload.kindle.stk.email.task.service

import arrow.core.Either
import org.grakovne.sideload.kindle.converter.ConvertationError
import org.grakovne.sideload.kindle.converter.StkLimitExhausted
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTask
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTaskStatus
import org.grakovne.sideload.kindle.stk.email.task.repository.TransferEmailTaskRepository
import org.grakovne.sideload.kindle.telegram.ConfigurationProperties
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Service
class TransferEmailTaskService(
    private val repository: TransferEmailTaskRepository,
    private val configurationProperties: ConfigurationProperties
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

        val userTodayTasks = findByUserAndDate(userId, LocalDate.now()).size

        if (userTodayTasks > configurationProperties.userStkDailyLimit) {
            return Either.Left(StkLimitExhausted)
        }

        return repository
            .save(entity)
            .let { Either.Right(Unit) }

    }

    fun fetchLatestForProcessing(): TransferEmailTask? =
        repository
            .findByStatusInAndCreatedAtLessThan(listOf(TransferEmailTaskStatus.ACTIVE), Instant.now())
            .firstOrNull()

    private fun findByUserAndDate(
        userId: String,
        date: LocalDate
    ): List<TransferEmailTask> {
        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        return repository.findByUserIdAndCreatedAtBetween(userId, startOfDay, endOfDay)
    }
}