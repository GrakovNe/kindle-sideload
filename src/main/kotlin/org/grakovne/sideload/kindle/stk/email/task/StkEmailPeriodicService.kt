package org.grakovne.sideload.kindle.stk.email.task

import arrow.core.Either
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.parallelMap
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.StkFinishedEvent
import org.grakovne.sideload.kindle.events.internal.StkFinishedStatus
import org.grakovne.sideload.kindle.common.mail.MailSendingService
import org.grakovne.sideload.kindle.stk.email.task.domain.SendingError
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailError
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTask
import org.grakovne.sideload.kindle.stk.email.task.domain.TransferEmailTaskStatus
import org.grakovne.sideload.kindle.stk.email.task.domain.UserEmailAbsent
import org.grakovne.sideload.kindle.stk.email.task.service.TransferEmailTaskService
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class StkEmailPeriodicService(
    private val userEnvironmentService: UserEnvironmentService,
    private val userPreferencesService: UserPreferencesService,
    private val mailSendingService: MailSendingService,
    private val taskService: TransferEmailTaskService,
    private val eventSender: EventSender
) {

    @Scheduled(fixedDelay = 100)
    fun stkEmail() {
        logger.trace { "Running periodically task ${this.javaClass.simpleName}" }

        runBlocking {
            taskService
                .fetchTasksForProcessing()
                .parallelMap { task ->
                    processTask(task)
                        .also { notifyUser(task, it) }
                        .fold(
                            ifLeft = { updateStatus(task, TransferEmailTaskStatus.FAILED, it.toString()) },
                            ifRight = { updateStatus(task, TransferEmailTaskStatus.SUCCESS) },
                        )
                }
        }
    }

    private fun notifyUser(task: TransferEmailTask, result: Either<TransferEmailError, Unit>) {
        val event = result
            .fold(
                ifLeft = {
                    StkFinishedEvent(
                        userId = task.userId,
                        status = StkFinishedStatus.FAILED
                    )
                },
                ifRight = {
                    StkFinishedEvent(
                        userId = task.userId,
                        status = StkFinishedStatus.SUCCESS
                    )
                }
            )

        eventSender.sendEvent(event)
    }

    private fun processTask(task: TransferEmailTask): Either<TransferEmailError, Unit> {
        val targetEmail = userPreferencesService
            .fetchPreferences(task.userId)
            .email
            ?: return Either.Left(UserEmailAbsent)

        val files = userEnvironmentService
            .provideEnvironmentFiles(task.environmentId)
            .mapNotNull { it }


        return mailSendingService
            .sendFile(
                address = targetEmail,
                files = files
            )
            .mapLeft { SendingError }
    }

    private fun updateStatus(
        task: TransferEmailTask,
        status: TransferEmailTaskStatus,
        failReason: String? = null
    ) =
        task
            .copy(status = status, failReason = failReason)
            .let { taskService.updateTask(it) }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}