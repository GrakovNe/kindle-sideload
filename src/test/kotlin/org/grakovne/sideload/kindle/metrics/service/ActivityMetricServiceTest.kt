package org.grakovne.sideload.kindle.metrics.service

import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.metrics.domain.PeriodicMetrics
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class ActivityMetricServiceTest {

    private val userService: UserService = mock()
    private val taskService: ConvertationTaskService = mock()
    private val sut = ActivityMetricService(userService, taskService)

    // The three windows share the same end of day but start one / eight / three
    // hundred and sixty six days apart, so the (from, to) span identifies the
    // bucket regardless of the current wall-clock time.
    private fun bucketOf(from: Instant, to: Instant) = when {
        Duration.between(from, to).toDays() <= 2 -> "today"
        Duration.between(from, to).toDays() <= 10 -> "weekly"
        else -> "yearly"
    }

    @Test
    fun `aggregates the user and conversion counts per time bucket`() {
        val users = (1..5).map { user() }
        val tasks = (1..9).map { task() }

        whenever(userService.fetchActiveUsers(any(), any())).thenAnswer { inv ->
            users.take(usersFor(bucketOf(inv.arguments[0] as Instant, inv.arguments[1] as Instant)))
        }
        whenever(taskService.fetchTasks(any(), any())).thenAnswer { inv ->
            tasks.take(tasksFor(bucketOf(inv.arguments[0] as Instant, inv.arguments[1] as Instant)))
        }

        val metrics = sut.aggregateMetrics()

        assertEquals(PeriodicMetrics(today = 1, weekly = 3, yearly = 5), metrics.users)
        assertEquals(PeriodicMetrics(today = 2, weekly = 7, yearly = 9), metrics.fileConvertations)
    }

    @Test
    fun `reports zero counts when there is no activity in any window`() {
        whenever(userService.fetchActiveUsers(any(), any())).thenReturn(emptyList<User>())
        whenever(taskService.fetchTasks(any(), any())).thenReturn(emptyList<ConvertationTask>())

        val metrics = sut.aggregateMetrics()

        assertEquals(PeriodicMetrics(today = 0, weekly = 0, yearly = 0), metrics.users)
        assertEquals(PeriodicMetrics(today = 0, weekly = 0, yearly = 0), metrics.fileConvertations)
    }

    private fun usersFor(bucket: String) = when (bucket) {
        "today" -> 1
        "weekly" -> 3
        else -> 5
    }

    private fun tasksFor(bucket: String) = when (bucket) {
        "today" -> 2
        "weekly" -> 7
        else -> 9
    }

    private fun user() = User(UUID.randomUUID().toString(), "en", Type.FREE_USER, null)

    private fun task() = ConvertationTask(
        id = UUID.randomUUID(),
        userId = "user-1",
        sourceFileUrl = "https://example.com/book.fb2",
        createdAt = Instant.now(),
        failReason = null,
        status = ConvertationTaskStatus.SUCCESS,
        fileName = "book.fb2"
    )
}
