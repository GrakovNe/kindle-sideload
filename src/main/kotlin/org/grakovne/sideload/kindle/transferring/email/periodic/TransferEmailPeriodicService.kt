package org.grakovne.sideload.kindle.transferring.email.periodic

import arrow.core.Either
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.mail.MailSendingService
import org.grakovne.sideload.kindle.transferring.email.domain.SendingError
import org.grakovne.sideload.kindle.transferring.email.domain.TransferEmailError
import org.grakovne.sideload.kindle.transferring.email.domain.TransferEmailTask
import org.grakovne.sideload.kindle.transferring.email.domain.TransferEmailTaskStatus
import org.grakovne.sideload.kindle.transferring.email.domain.UserEmailAbsent
import org.grakovne.sideload.kindle.transferring.email.service.TransferEmailTaskService
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TransferEmailPeriodicService(
    private val userEnvironmentService: UserEnvironmentService,
    private val userPreferencesService: UserPreferencesService,
    private val mailSendingService: MailSendingService,
    private val taskService: TransferEmailTaskService
) {

    @Scheduled(fixedDelay = 100)
    fun sendRequestedEmails() {
        taskService
            .fetchTasksForProcessing()
            .map { task ->
                processTask(task)
                    .fold(
                        ifLeft = { updateStatus(task, TransferEmailTaskStatus.FAILED, it.toString()) },
                        ifRight = { updateStatus(task, TransferEmailTaskStatus.SUCCESS) },
                    )
            }
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

}