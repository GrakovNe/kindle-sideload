package org.grakovne.sideload.kindle.converer.task.periodic

import arrow.core.Either
import org.grakovne.sideload.kindle.converer.ConvertationError
import org.grakovne.sideload.kindle.converer.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converer.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converer.task.service.ConvertationTaskService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ConvertSourceFilePeriodicService(
    private val taskService: ConvertationTaskService
) {

    @Scheduled(fixedDelay = 5000)
    fun convertSourceFiles() {

        taskService
            .fetchTasksForProcessing()
            .map { it to processTask(it) }
            .map { (task, result) ->
                notifyUser(task, result)
                task to result
            }
            .map { (task, result) ->
                updateStatus(task, result)
            }

    }

    private fun updateStatus(task: ConvertationTask, result: Either<ConvertationError, Unit>) {
        val entity = when (result) {
            is Either.Left -> task.copy(status = ConvertationTaskStatus.FAILED, failReason = result.toString())
            is Either.Right -> task.copy(status = ConvertationTaskStatus.SUCCESS)
        }

        taskService.updateTask(entity)
    }

    private fun notifyUser(task: ConvertationTask, result: Either<ConvertationError, Unit>) {
        // not now
    }

    private fun processTask(task: ConvertationTask): Either<ConvertationError, Unit> {
        println()

        return Either.Right(Unit)
    }
}