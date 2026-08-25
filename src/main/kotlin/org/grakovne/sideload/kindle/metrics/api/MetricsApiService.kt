package org.grakovne.sideload.kindle.metrics.api

import org.grakovne.sideload.kindle.metrics.api.domain.DailyMetrics
import org.grakovne.sideload.kindle.metrics.api.domain.UserDailyMetrics
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converter.task.repository.ConvertationTaskRepository
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTaskStatus
import org.grakovne.sideload.kindle.stk.email.task.repository.TransferEmailTaskRepository
import org.grakovne.sideload.kindle.user.message.report.repository.UserMessageReportRepository
import org.grakovne.sideload.kindle.user.reference.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC

@Service
class MetricsApiService(
    private val convertationTaskRepository: ConvertationTaskRepository,
    private val transferEmailTaskRepository: TransferEmailTaskRepository,
    private val userMessageReportRepository: UserMessageReportRepository,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun fetchDailyMetrics(): DailyMetrics {
        val (from, to) = dayWindow(LocalDate.now(UTC))

        return DailyMetrics(
            convertedBooks = convertationTaskRepository
                .findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(ConvertationTaskStatus.SUCCESS, from, to)
                .count(),
            failedBooks = convertationTaskRepository
                .findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(ConvertationTaskStatus.FAILED, from, to)
                .count(),
            sentEmails = transferEmailTaskRepository
                .findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(TransferEmailTaskStatus.SUCCESS, from, to)
                .count(),
            failedEmails = transferEmailTaskRepository
                .findByFailReasonIsNotNullAndCreatedAtGreaterThanAndCreatedAtLessThan(from, to)
                .count(),
            users = userMessageReportRepository
                .findByCreatedAtGreaterThanAndCreatedAtLessThan(from, to)
                .groupBy { it.userId }
                .map { (userId, messages) -> UserDailyMetrics(userId = userId, sentMessages = messages.count()) }
                .sortedByDescending { it.sentMessages }
                .also { users ->
                    users
                        .map { it.userId }
                        .let { ids -> ids.distinct() }
                        .forEach { id -> userRepository.touchLastActivity(id, Instant.now()) }
                }
        )
    }

    private fun dayWindow(date: LocalDate): Pair<Instant, Instant> =
        date.atStartOfDay(UTC).toInstant() to date.plusDays(1).atStartOfDay(UTC).toInstant()
}
