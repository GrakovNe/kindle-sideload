package org.grakovne.sideload.kindle.metrics.api

import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converter.task.repository.ConvertationTaskRepository
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTask
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTaskStatus
import org.grakovne.sideload.kindle.stk.email.task.repository.TransferEmailTaskRepository
import org.grakovne.sideload.kindle.user.message.report.domain.UserMessageReport
import org.grakovne.sideload.kindle.user.message.report.repository.UserMessageReportRepository
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.annotation.Commit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
class MetricsRepositoryTest {

    @Autowired
    lateinit var convertationTaskRepository: ConvertationTaskRepository

    @Autowired
    lateinit var transferEmailTaskRepository: TransferEmailTaskRepository

    @Autowired
    lateinit var userMessageReportRepository: UserMessageReportRepository

    @Autowired
    lateinit var userRepository: UserRepository

    private val inside = Instant.parse("2026-08-25T06:00:00Z")
    private val outside = Instant.parse("2026-08-24T06:00:00Z")
    private val from = Instant.parse("2026-08-25T00:00:00Z")
    private val to = Instant.parse("2026-08-26T00:00:00Z")

    @BeforeEach
    fun cleanUp() {
        convertationTaskRepository.deleteAll()
        transferEmailTaskRepository.deleteAll()
        userMessageReportRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `fetches the convertation tasks by status inside the day window`() {
        convertationTaskRepository.saveAll(
            listOf(
                convertationTask(ConvertationTaskStatus.SUCCESS, inside),
                convertationTask(ConvertationTaskStatus.SUCCESS, inside),
                convertationTask(ConvertationTaskStatus.FAILED, inside),
                convertationTask(ConvertationTaskStatus.SUCCESS, outside)
            )
        )

        assertEquals(2, convertationTaskRepository.findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(ConvertationTaskStatus.SUCCESS, from, to).count())
        assertEquals(1, convertationTaskRepository.findByStatusAndCreatedAtGreaterThanAndCreatedAtLessThan(ConvertationTaskStatus.FAILED, from, to).count())
    }

    @Test
    fun `fetches the transfer email tasks inside the day window split by the fail reason`() {
        transferEmailTaskRepository.saveAll(
            listOf(
                transferEmailTask(TransferEmailTaskStatus.SUCCESS, null, inside),
                transferEmailTask(TransferEmailTaskStatus.SUCCESS, null, inside),
                transferEmailTask(TransferEmailTaskStatus.FAILED, "smtp down", inside),
                transferEmailTask(TransferEmailTaskStatus.SUCCESS, null, outside)
            )
        )

        val tasks = transferEmailTaskRepository.findByCreatedAtGreaterThanAndCreatedAtLessThan(from, to)
        assertEquals(3, tasks.count())

        assertEquals(1, transferEmailTaskRepository.findByFailReasonIsNotNullAndCreatedAtGreaterThanAndCreatedAtLessThan(from, to).count())
    }

    @Test
    fun `fetches the user messages inside the day window`() {
        userMessageReportRepository.saveAll(
            listOf(
                userMessage("user-1", inside),
                userMessage("user-2", inside),
                userMessage("user-1", outside)
            )
        )

        val messages = userMessageReportRepository.findByCreatedAtGreaterThanAndCreatedAtLessThan(from, to)
        assertEquals(2, messages.count())
        assertEquals(
            listOf("user-1", "user-2"),
            messages.map { it.userId }
        )
    }

    @Commit
    @Test
    fun `touches the last activity timestamp of the user`() {
        userRepository.save(user("user-1", Instant.parse("2026-01-01T00:00:00Z")))

        val rows: Int = userRepository.touchLastActivity("user-1", inside)

        assertEquals(1, rows)
        assertEquals(inside, userRepository.findById("user-1").get().lastActivityTimestamp)
    }

    @Commit
    @Test
    fun `the touched user becomes active for the bot metrics window`() {
        userRepository.save(user("user-1", Instant.parse("2026-01-01T00:00:00Z")))

        userRepository.touchLastActivity("user-1", inside)

        val active = userRepository.findByLastActivityTimestampGreaterThanAndLastActivityTimestampLessThan(from, to)
        assertEquals(listOf("user-1"), active.map { it.id })
        assertTrue(userRepository.findByType(Type.FREE_USER).map { it.id }.contains("user-1"))
    }

    private fun user(id: String, lastActivity: Instant?) = User(
        id = id,
        language = "en",
        type = Type.FREE_USER,
        lastActivityTimestamp = lastActivity
    )

    private fun convertationTask(status: ConvertationTaskStatus, createdAt: Instant) = ConvertationTask(
        id = UUID.randomUUID(),
        userId = "user-1",
        sourceFileUrl = "https://example.com/book.fb2",
        createdAt = createdAt,
        failReason = null,
        status = status,
        fileName = "book.fb2"
    )

    private fun transferEmailTask(status: TransferEmailTaskStatus, failReason: String?, createdAt: Instant) = TransferEmailTask(
        id = UUID.randomUUID(),
        userId = "user-1",
        environmentId = "env-1",
        createdAt = createdAt,
        failReason = failReason,
        status = status
    )

    private fun userMessage(userId: String, createdAt: Instant) = UserMessageReport(
        id = UUID.randomUUID(),
        userId = userId,
        createdAt = createdAt,
        text = "text"
    )
}
