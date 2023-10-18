package org.grakovne.sideload.kindle.converer.task.periodic

import arrow.core.Either
import org.grakovne.sideload.kindle.common.FileDownloadService
import org.grakovne.sideload.kindle.converer.ConversionResult
import org.grakovne.sideload.kindle.converer.ConvertationError
import org.grakovne.sideload.kindle.converer.ConverterService
import org.grakovne.sideload.kindle.converer.UnableFetchFile
import org.grakovne.sideload.kindle.converer.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converer.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converer.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ConvertSourceFilePeriodicService(
    private val downloadService: FileDownloadService,
    private val converterService: ConverterService,
    private val taskService: ConvertationTaskService,
    private val eventSender: EventSender
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
                //updateStatus(task, result)
            }

    }

    private fun updateStatus(task: ConvertationTask, result: Either<ConvertationError, ConversionResult>) {
        val entity = when (result) {
            is Either.Left -> task.copy(status = ConvertationTaskStatus.FAILED, failReason = result.toString())
            is Either.Right -> task.copy(status = ConvertationTaskStatus.ACTIVE)
        }

        taskService.updateTask(entity)
    }

    private fun notifyUser(task: ConvertationTask, result: Either<ConvertationError, ConversionResult>) {
        val event = result
            .fold(
                ifLeft = {
                    ConvertationFinishedEvent(
                        userId = task.userId,
                        status = ConvertationFinishedStatus.FAILED,
                        log = it.details ?: "",
                        output = emptyList()
                    )
                },
                ifRight = {
                    ConvertationFinishedEvent(
                        userId = task.userId,
                        status = ConvertationFinishedStatus.SUCCESS,
                        log = it.log,
                        output = it.output
                    )
                }
            )

        eventSender.sendEvent(event)
    }

    private fun processTask(task: ConvertationTask): Either<ConvertationError, ConversionResult> {
        val file = downloadService.download(task.sourceFileUrl)
            ?: return Either.Left(UnableFetchFile)


        return converterService.processAndCollect(task.userId, file)
    }
}