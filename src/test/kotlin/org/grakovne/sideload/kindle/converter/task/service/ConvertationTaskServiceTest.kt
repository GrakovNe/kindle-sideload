package org.grakovne.sideload.kindle.converter.task.service

import arrow.core.Either
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converter.task.repository.ConvertationTaskDao
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class ConvertationTaskServiceTest {

    private val repository: ConvertationTaskDao = mock()
    private val sut = ConvertationTaskService(repository)

    @Test
    fun `fetches tasks by the created at window`() {
        val from = Instant.parse("2026-08-01T00:00:00Z")
        val to = Instant.parse("2026-08-02T00:00:00Z")
        val tasks = listOf(task(status = ConvertationTaskStatus.ACTIVE))
        whenever(repository.findByCreatedAtGreaterThanAndCreatedAtLessThan(from, to)).thenReturn(tasks)

        val result = sut.fetchTasks(from, to)

        assertEquals(tasks, result)
    }

    @Test
    fun `updates the task and returns a right result`() {
        val task = task(status = ConvertationTaskStatus.FAILED)
        whenever(repository.save(task)).thenReturn(task)

        val result = sut.updateTask(task)

        assertEquals(Either.Right(Unit), result)
    }

    @Test
    fun `submits an active task for the user`() {
        val user = User(id = "user-1", language = null, type = Type.FREE_USER, lastActivityTimestamp = null)
        whenever(repository.save(any<ConvertationTask>()))
            .thenAnswer { it.arguments.first() as ConvertationTask }

        val result = sut.submitTask(user, "https://example.com/book.fb2", "book.fb2")

        assertEquals(Either.Right(Unit), result)
        val captor = argumentCaptor<ConvertationTask>()
        verify(repository).save(captor.capture())
        val saved = captor.firstValue
        assertEquals("user-1", saved.userId)
        assertEquals("https://example.com/book.fb2", saved.sourceFileUrl)
        assertEquals("book.fb2", saved.fileName)
        assertEquals(ConvertationTaskStatus.ACTIVE, saved.status)
        assertEquals(null, saved.failReason)
    }

    @Test
    fun `fetches only active tasks for processing`() {
        whenever(repository.findByStatusInAndCreatedAtLessThan(
            eq(listOf(ConvertationTaskStatus.ACTIVE)), any()
        )).thenReturn(listOf(task(status = ConvertationTaskStatus.ACTIVE)))

        val result = sut.fetchTasksForProcessing()

        assertEquals(1, result.size)
    }

    private fun task(status: ConvertationTaskStatus) = ConvertationTask(
        id = UUID.randomUUID(),
        userId = "user-1",
        sourceFileUrl = "https://example.com/book.fb2",
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        failReason = null,
        status = status,
        fileName = "book.fb2"
    )
}
