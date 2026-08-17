package org.grakovne.sideload.kindle.converter.task.repository

import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.Instant
import java.util.UUID

@DataJpaTest
class ConvertationTaskRepositoryTest {

    @Autowired
    lateinit var repository: ConvertationTaskRepository

    @Test
    fun `finds tasks created within the given window`() {
        repository.saveAll(
            listOf(
                task(createdAt = Instant.parse("2026-08-01T00:00:00Z")),
                task(createdAt = Instant.parse("2026-08-03T00:00:00Z")),
                task(createdAt = Instant.parse("2026-08-05T00:00:00Z"))
            )
        )

        val found = repository.findByCreatedAtGreaterThanAndCreatedAtLessThan(
            Instant.parse("2026-08-02T00:00:00Z"),
            Instant.parse("2026-08-04T00:00:00Z")
        )

        val createdAt = found.map { it.createdAt }
        assertOnly(listOf(Instant.parse("2026-08-03T00:00:00Z")), createdAt)
    }

    @Test
    fun `finds active tasks created before the given instant`() {
        repository.saveAll(
            listOf(
                task(createdAt = Instant.parse("2026-08-01T00:00:00Z"), status = ConvertationTaskStatus.ACTIVE),
                task(createdAt = Instant.parse("2026-08-02T00:00:00Z"), status = ConvertationTaskStatus.ACTIVE),
                task(createdAt = Instant.parse("2026-08-03T00:00:00Z"), status = ConvertationTaskStatus.SUCCESS),
                task(createdAt = Instant.parse("2026-08-04T00:00:00Z"), status = ConvertationTaskStatus.ACTIVE)
            )
        )

        val found = repository.findByStatusInAndCreatedAtLessThan(
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
        repository.save(task(createdAt = Instant.parse("2026-08-01T00:00:00Z"), status = ConvertationTaskStatus.SUCCESS))

        val found = repository.findByStatusInAndCreatedAtLessThan(
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
