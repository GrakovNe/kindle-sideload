package org.grakovne.sideload.kindle.metrics.api

import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converter.task.repository.ConvertationTaskRepository
import org.grakovne.sideload.kindle.metrics.api.domain.DailyMetrics
import org.grakovne.sideload.kindle.metrics.api.domain.UserDailyMetrics
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTask
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTaskStatus
import org.grakovne.sideload.kindle.stk.email.task.repository.TransferEmailTaskRepository
import org.grakovne.sideload.kindle.user.message.report.domain.UserMessageReport
import org.grakovne.sideload.kindle.user.message.report.repository.UserMessageReportRepository
import org.grakovne.sideload.kindle.user.reference.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetricsApiServiceTest {

    private val convertationTaskRepository: ConvertationTaskRepository = mock()
    private val transferEmailTaskRepository: TransferEmailTaskRepository = mock()
    private val userMessageReportRepository: UserMessageReportRepository = mock()
    private val userRepository: UserRepository = mock()
    private val sut = MetricsApiService(
        convertationTaskRepository,
        transferEmailTaskRepository,
        userMessageReportRepository,
        userRepository
    )

    private val now = Instant.now()

    @Test
    fun `aggregates the daily counts and groups the users by the sent messages`() {
        whenever(convertationTaskRepository.findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(any(), any(), any())).thenAnswer { inv ->
            if (inv.arguments[0] == ConvertationTaskStatus.SUCCESS) (1..3).map { convertationTask(ConvertationTaskStatus.SUCCESS) } else (1..2).map { convertationTask(ConvertationTaskStatus.FAILED) }
        }
        whenever(transferEmailTaskRepository.findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(eq(TransferEmailTaskStatus.SUCCESS), any(), any())).thenReturn((1..4).map { transferEmailTask() })
        whenever(transferEmailTaskRepository.findByFailReasonIsNotNullAndCreatedAtGreaterThanAndCreatedAtLessThan(any(), any())).thenReturn((1..2).map { transferEmailTask("smtp down") })
        whenever(userMessageReportRepository.findByCreatedAtGreaterThanAndCreatedAtLessThan(any(), any())).thenReturn(
            listOf(
                message("user-1"), message("user-1"), message("user-1"),
                message("user-2")
            )
        )

        val metrics = sut.fetchDailyMetrics()

        assertEquals(3, metrics.convertedBooks)
        assertEquals(2, metrics.failedBooks)
        assertEquals(4, metrics.sentEmails)
        assertEquals(2, metrics.failedEmails)
        assertEquals(
            listOf(
                UserDailyMetrics(userId = "user-1", sentMessages = 3),
                UserDailyMetrics(userId = "user-2", sentMessages = 1)
            ),
            metrics.users
        )

        verify(userRepository).touchLastActivity(eq("user-1"), any<Instant>())
        verify(userRepository).touchLastActivity(eq("user-2"), any<Instant>())
    }

    @Test
    fun `reports zero counts and no users when there is no activity in the day`() {
        whenever(convertationTaskRepository.findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(any(), any(), any())).thenReturn(emptyList<ConvertationTask>())
        whenever(transferEmailTaskRepository.findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(eq(TransferEmailTaskStatus.SUCCESS), any(), any())).thenReturn(emptyList<TransferEmailTask>())
        whenever(transferEmailTaskRepository.findByFailReasonIsNotNullAndCreatedAtGreaterThanAndCreatedAtLessThan(any(), any())).thenReturn(emptyList<TransferEmailTask>())
        whenever(userMessageReportRepository.findByCreatedAtGreaterThanAndCreatedAtLessThan(any(), any())).thenReturn(emptyList<UserMessageReport>())

        val metrics = sut.fetchDailyMetrics()

        assertEquals(0, metrics.convertedBooks)
        assertEquals(0, metrics.failedBooks)
        assertEquals(0, metrics.sentEmails)
        assertEquals(0, metrics.failedEmails)
        assertEquals(emptyList<UserDailyMetrics>(), metrics.users)
    }

    @Test
    fun `counts only the activity inside the requested day`() {
        val fromCaptor = argumentCaptor<Instant>()
        val toCaptor = argumentCaptor<Instant>()

        whenever(convertationTaskRepository.findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(any(), fromCaptor.capture(), toCaptor.capture())).thenReturn(emptyList<ConvertationTask>())
        whenever(transferEmailTaskRepository.findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(eq(TransferEmailTaskStatus.SUCCESS), any(), any())).thenReturn(emptyList<TransferEmailTask>())
        whenever(transferEmailTaskRepository.findByFailReasonIsNotNullAndCreatedAtGreaterThanAndCreatedAtLessThan(any(), any())).thenReturn(emptyList<TransferEmailTask>())
        whenever(userMessageReportRepository.findByCreatedAtGreaterThanAndCreatedAtLessThan(any(), any())).thenReturn(emptyList<UserMessageReport>())

        sut.fetchDailyMetrics()

        val from = fromCaptor.firstValue
        val to = toCaptor.firstValue

        assertEquals(Duration.ofDays(1), Duration.between(from, to))
        assertTrue(now.isAfter(from) && (now.isBefore(to) || now == to))
    }

    private fun message(userId: String) = UserMessageReport(
        id = UUID.randomUUID(),
        userId = userId,
        createdAt = now,
        text = "text"
    )

    private fun convertationTask(status: ConvertationTaskStatus) = ConvertationTask(
        id = UUID.randomUUID(),
        userId = "user-1",
        sourceFileUrl = "https://example.com/book.fb2",
        createdAt = now,
        failReason = null,
        status = status,
        fileName = "book.fb2"
    )

    private fun transferEmailTask(failReason: String? = null) = TransferEmailTask(
        id = UUID.randomUUID(),
        userId = "user-1",
        environmentId = "env-1",
        createdAt = now,
        failReason = failReason,
        status = if (failReason == null) TransferEmailTaskStatus.SUCCESS else TransferEmailTaskStatus.FAILED
    )
}
