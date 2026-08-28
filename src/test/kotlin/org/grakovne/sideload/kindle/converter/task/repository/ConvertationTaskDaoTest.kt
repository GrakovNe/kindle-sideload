package org.grakovne.sideload.kindle.converter.task.repository

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.UUID

class ConvertationTaskDaoTest : TestDatabase() {

    @Autowired
    lateinit var dao: ConvertationTaskDao

    @Test
    fun `finds tasks created within the given window`() {
        dao.saveAll(
            listOf(
                task(createdAt = Instant.parse("2026-08-01T00:00:00Z")),
                task(createdAt = Instant.parse("2026-08-03T00:00:00Z")),
                task(createdAt = Instant.parse("2026-08-05T00:00:00Z"))
            )
        )

        val found = dao.findByCreatedAtGreaterThanAndCreatedAtLessThan(
            Instant.parse("2026-08-02T00:00:00Z"),
            Instant.parse("2026-08-04T00:00:00Z")
        )

        val createdAt = found.map { it.createdAt }
        assertOnly(listOf(Instant.parse("2026-08-03T00:00:00Z")), createdAt)
    }

    @Test
    fun `finds active tasks created before the given instant`() {
        dao.saveAll(
            listOf(
                task(createdAt = Instant.parse("2026-08-01T00:00:00Z"), status = ConvertationTaskStatus.ACTIVE),
                task(createdAt = Instant.parse("2026-08-02T00:00:00Z"), status = ConvertationTaskStatus.ACTIVE),
                task(createdAt = Instant.parse("2026-08-03T00:00:00Z"), status = ConvertationTaskStatus.SUCCESS),
                task(createdAt = Instant.parse("2026-08-04T00:00:00Z"), status = ConvertationTaskStatus.ACTIVE)
            )
        )

        val found = dao.findByStatusInAndCreatedAtLessThan(
            listOf(ConvertationTaskStatus.ACTIVE),
            Instant.parse("2026-08-04T00:00:00Z")
        )

        val createdAt = found.map { it.createdAt }
        assertOnly(
            listOf(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-02T00:00:00Z")
            ),
            createdAt
        )
    }

    @Test
    fun `finds nothing when there are no matching tasks`() {
        dao.save(task(createdAt = Instant.parse("2026-08-01T00:00:00Z"), status = ConvertationTaskStatus.SUCCESS))

        val found = dao.findByStatusInAndCreatedAtLessThan(
            listOf(ConvertationTaskStatus.ACTIVE),
            Instant.parse("2026-08-04T00:00:00Z")
        )

        assertOnly(emptyList(), found.map { it.createdAt })
    }

    private fun assertOnly(expected: List<Instant>, actual: List<Instant>) {
        if (expected.sorted() != actual.sorted()) {
            throw AssertionError("expected $expected but was $actual")
        }
    }

    private fun task(createdAt: Instant, status: ConvertationTaskStatus = ConvertationTaskStatus.ACTIVE) =
        ConvertationTask(
            id = UUID.randomUUID(),
            userId = "user-1",
            sourceFileUrl = "https://example.com/book.fb2",
            createdAt = createdAt,
            failReason = null,
            status = status,
            fileName = "book.fb2"
        )
}
