package org.grakovne.sideload.kindle.metrics.service

import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.metrics.domain.PeriodicMetrics
import org.grakovne.sideload.kindle.metrics.domain.ActivityMetrics
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset.UTC

@Service
class ActivityMetricService(
    private val userService: UserService,
    private val taskService: ConvertationTaskService,
) {

    fun aggregateMetrics(): ActivityMetrics {
        val activeUsers = PeriodicMetrics(
            today = countUsers(MetricTimeRange.TODAY),
            weekly = countUsers(MetricTimeRange.LAST_WEEK),
            yearly = countUsers(MetricTimeRange.YEAR),
        )

        val convertations = PeriodicMetrics(
            today = countConvertations(MetricTimeRange.TODAY),
            weekly = countConvertations(MetricTimeRange.LAST_WEEK),
            yearly = countConvertations(MetricTimeRange.YEAR),
        )
        
        return ActivityMetrics(
            users = activeUsers,
            fileConvertations = convertations
        )
    }


    private fun countUsers(range: MetricTimeRange) = range
        .toTimeFrame()
        .let { (from, to) -> userService.fetchActiveUsers(from, to).count() }

    private fun countConvertations(range: MetricTimeRange) = range
        .toTimeFrame()
        .let { (from, to) -> taskService.fetchTasks(from, to).count() }
}

enum class MetricTimeRange {
    TODAY,
    LAST_WEEK,
    YEAR
}

private fun MetricTimeRange.toTimeFrame(): Pair<Instant, Instant> {
    val now = Instant.now()

    return when (this) {
        MetricTimeRange.TODAY -> now.atStartOfDay() to now.atEndOfDay()
        MetricTimeRange.LAST_WEEK -> now.minus(Duration.ofDays(7)).atStartOfDay() to now.atEndOfDay()
        MetricTimeRange.YEAR -> now.minus(Duration.ofDays(365)).atStartOfDay() to now.atEndOfDay()
    }
}

private fun Instant.atStartOfDay() = this.atOffset(UTC).toLocalDate().atStartOfDay().toInstant(UTC)
private fun Instant.atEndOfDay() = this.atOffset(UTC).toLocalDate().plusDays(1).atStartOfDay().toInstant(UTC)