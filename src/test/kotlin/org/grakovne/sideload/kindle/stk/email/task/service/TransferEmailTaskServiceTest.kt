package org.grakovne.sideload.kindle.stk.email.task.service

import ch.qos.logback.classic.Level
import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.converter.StkLimitExhausted
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTask
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTaskStatus
import org.grakovne.sideload.kindle.stk.email.task.repository.TransferEmailTaskDao
import org.grakovne.sideload.kindle.telegram.ConfigurationProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransferEmailTaskServiceTest : TestDatabase() {

    @Autowired
    lateinit var dao: TransferEmailTaskDao

    private lateinit var sut: TransferEmailTaskService

    @BeforeEach
    fun setUp() {
        val properties = ConfigurationProperties()
        properties.token = "test-token"
        properties.level = Level.INFO
        properties.userStkDailyLimit = 2
        sut = TransferEmailTaskService(dao, properties)
    }

    @Test
    fun `submits an active task with the user and environment`() {
        val result = sut.submitTask("user-1", "env-1")

        assertTrue(result.isRight())
        val task = dao.findAll().single()
        assertEquals("user-1", task.userId)
        assertEquals("env-1", task.environmentId)
        assertEquals(TransferEmailTaskStatus.ACTIVE, task.status)
        assertNull(task.failReason)
    }

    @Test
    fun `rejects the submission once the daily allowance is exceeded`() {
        // the limit is enforced with a strict '>', so up to limit + 1 tasks
        // are accepted before StkLimitExhausted is returned
        assertTrue(sut.submitTask("user-1", "env-1").isRight())
        assertTrue(sut.submitTask("user-1", "env-2").isRight())
        assertTrue(sut.submitTask("user-1", "env-3").isRight())

        val result = sut.submitTask("user-1", "env-4")

        assertTrue(result.isLeft())
        assertEquals(StkLimitExhausted, result.swap().getOrNull())
        assertEquals(3, dao.count())
    }

    @Test
    fun `does not count tasks from another day towards the limit`() {
        dao.save(
            task(
                userId = "user-1",
                createdAt = Instant.now().minus(48, ChronoUnit.HOURS)
            )
        )

        val result = sut.submitTask("user-1", "env-1")

        assertTrue(result.isRight())
        assertEquals(2, dao.count())
    }

    @Test
    fun `counts limits per user`() {
        dao.save(task(userId = "user-2", createdAt = Instant.now()))
        dao.save(task(userId = "user-2", createdAt = Instant.now()))

        val result = sut.submitTask("user-1", "env-1")

        assertTrue(result.isRight())
    }

    @Test
    fun `fetches the active task for processing`() {
        dao.save(task("user-1", TransferEmailTaskStatus.SUCCESS))
        val active = task("user-2", TransferEmailTaskStatus.ACTIVE)
        dao.save(active)

        val fetched = sut.fetchLatestForProcessing()

        assertEquals(active.id, fetched?.id)
    }

    @Test
    fun `returns no task when the queue is empty`() {
        assertNull(sut.fetchLatestForProcessing())
    }

    @Test
    fun `persists task updates`() {
        val task = task("user-1", TransferEmailTaskStatus.ACTIVE)
        dao.save(task)

        sut.updateTask(task.copy(status = TransferEmailTaskStatus.FAILED, failReason = "boom"))

        val stored = dao.findById(task.id)!!
        assertEquals(TransferEmailTaskStatus.FAILED, stored.status)
        assertEquals("boom", stored.failReason)
    }

    // createdAt is put in the past so the strict `createdAt < now` window of
    // fetchLatestForProcessing cannot race with the fetch instant
    private fun task(
        userId: String,
        status: TransferEmailTaskStatus = TransferEmailTaskStatus.ACTIVE,
        createdAt: Instant = Instant.now().minusSeconds(1)
    ) = TransferEmailTask(
        id = UUID.randomUUID(),
        userId = userId,
        environmentId = "env-1",
        createdAt = createdAt,
        failReason = null,
        status = status
    )
}
