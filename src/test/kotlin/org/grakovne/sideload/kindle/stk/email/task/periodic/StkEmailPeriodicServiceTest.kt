package org.grakovne.sideload.kindle.stk.email.task.periodic

import arrow.core.Either
import org.grakovne.sideload.kindle.common.mail.MailError
import org.grakovne.sideload.kindle.common.mail.MailSendingService
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.StkFinishedEvent
import org.grakovne.sideload.kindle.events.internal.StkFinishedStatus
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTask
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTaskStatus
import org.grakovne.sideload.kindle.stk.email.task.service.TransferEmailTaskService
import org.grakovne.sideload.kindle.user.common.OutputFormat
import org.grakovne.sideload.kindle.user.preferences.domain.UserPreferences
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StkEmailPeriodicServiceTest {

    private val userEnvironmentService = mock<UserEnvironmentService>()
    private val userPreferencesService = mock<UserPreferencesService>()
    private val mailSendingService = mock<MailSendingService>()
    private val taskService = mock<TransferEmailTaskService>()
    private val eventSender = mock<EventSender>()

    private lateinit var sut: StkEmailPeriodicService

    @BeforeEach
    fun setUp() {
        sut = StkEmailPeriodicService(
            userEnvironmentService,
            userPreferencesService,
            mailSendingService,
            taskService,
            eventSender
        )
    }

    @Test
    fun `sends the environment files to the user e-mail and marks the task as successful`() {
        val task = task()
        val attachments = listOf(File("/env/1/book.azw3"), File("/env/1/cover.jpg"))

        whenever(taskService.fetchLatestForProcessing()).thenReturn(task)
        whenever(userPreferencesService.fetchPreferences(eq(task.userId)))
            .thenReturn(preferences(email = "kindle-user@example.com"))
        whenever(userEnvironmentService.provideEnvironmentFiles(eq(task.environmentId)))
            .thenReturn(attachments)
        whenever(mailSendingService.sendFile(any<String>(), any<List<File>>()))
            .thenReturn(Either.Right(Unit))

        sut.stkEmail()

        val filesCaptor: KArgumentCaptor<List<File>> = argumentCaptor()
        verify(mailSendingService).sendFile(eq("kindle-user@example.com"), filesCaptor.capture())
        assertEquals(attachments, filesCaptor.firstValue)
        verifyUpdatedTask(task, TransferEmailTaskStatus.SUCCESS, null)
        verifyEventStkFinished(task.userId, StkFinishedStatus.SUCCESS)
    }

    @Test
    fun `fails the task with the user email absent error when the user has no e-mail`() {
        val task = task()

        whenever(taskService.fetchLatestForProcessing()).thenReturn(task)
        whenever(userPreferencesService.fetchPreferences(eq(task.userId)))
            .thenReturn(preferences(email = null))

        sut.stkEmail()

        verifyUpdatedTask(task, TransferEmailTaskStatus.FAILED, "UserEmailAbsent")
        verifyEventStkFinished(task.userId, StkFinishedStatus.FAILED)
    }

    @Test
    fun `fails the task with the sending error when the mail delivery fails`() {
        val task = task()

        whenever(taskService.fetchLatestForProcessing()).thenReturn(task)
        whenever(userPreferencesService.fetchPreferences(eq(task.userId)))
            .thenReturn(preferences(email = "kindle-user@example.com"))
        whenever(userEnvironmentService.provideEnvironmentFiles(eq(task.environmentId)))
            .thenReturn(listOf(File("/env/1/book.azw3")))
        whenever(mailSendingService.sendFile(any<String>(), any<List<File>>()))
            .thenReturn(Either.Left(MailError.DELIVERY_ERROR))

        sut.stkEmail()

        verifyUpdatedTask(task, TransferEmailTaskStatus.FAILED, "SendingError")
        verifyEventStkFinished(task.userId, StkFinishedStatus.FAILED)
    }

    @Test
    fun `fails the task with the sending error when the mail sender throws`() {
        val task = task()

        whenever(taskService.fetchLatestForProcessing()).thenReturn(task)
        whenever(userPreferencesService.fetchPreferences(eq(task.userId)))
            .thenReturn(preferences(email = "kindle-user@example.com"))
        whenever(userEnvironmentService.provideEnvironmentFiles(eq(task.environmentId)))
            .thenReturn(listOf(File("/env/1/book.azw3")))
        doThrow(RuntimeException("smtp exploded")).whenever(mailSendingService)
            .sendFile(any<String>(), any<List<File>>())

        sut.stkEmail()

        verifyUpdatedTask(task, TransferEmailTaskStatus.FAILED, "SendingError")
        verifyEventStkFinished(task.userId, StkFinishedStatus.FAILED)
    }

    @Test
    fun `does nothing when there is no task for processing`() {
        whenever(taskService.fetchLatestForProcessing()).thenReturn(null)

        sut.stkEmail()

        verify(taskService, never()).updateTask(isA<TransferEmailTask>())
        verify(eventSender, never()).sendEvent(isA<Event>())
    }

    private fun verifyUpdatedTask(
        task: TransferEmailTask,
        status: TransferEmailTaskStatus,
        expectedFailReason: String?
    ) {
        val captor = argumentCaptor<TransferEmailTask>()
        verify(taskService).updateTask(captor.capture())
        val updated = captor.firstValue
        assertEquals(task.id, updated.id)
        assertEquals(status, updated.status)
        if (expectedFailReason == null) {
            assertNull(updated.failReason)
        } else {
            // the error's default toString (Class@hash) is stored verbatim,
            // so assert on the class name embedded in it
            assertTrue(updated.failReason!!.contains(expectedFailReason))
        }
    }

    private fun verifyEventStkFinished(userId: String, status: StkFinishedStatus) {
        val eventCaptor: KArgumentCaptor<StkFinishedEvent> = argumentCaptor()
        verify(eventSender).sendEvent(eventCaptor.capture())
        val event = eventCaptor.firstValue
        assertEquals(userId, event.userId)
        assertEquals(status, event.status)
    }

    private fun task(
        userId: String = "user-1",
        environmentId: String = "env-1"
    ) = TransferEmailTask(
        id = UUID.randomUUID(),
        userId = userId,
        environmentId = environmentId,
        createdAt = Instant.now(),
        failReason = null,
        status = TransferEmailTaskStatus.ACTIVE
    )

    private fun preferences(email: String?) = UserPreferences(
        id = UUID.randomUUID(),
        userId = "user-1",
        outputFormat = OutputFormat.EPUB,
        email = email,
        debugMode = false,
        automaticStk = false
    )
}
