package org.grakovne.sideload.kindle.converter.task.periodic

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.FileDownloadService
import org.grakovne.sideload.kindle.converter.ConversionResult
import org.grakovne.sideload.kindle.converter.ConvertationError
import org.grakovne.sideload.kindle.converter.ConverterService
import org.grakovne.sideload.kindle.converter.UnableFetchFile
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
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
        logger.debug { "Running periodically task ${this.javaClass.simpleName}" }

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

    private fun updateStatus(task: ConvertationTask, result: Either<ConvertationError, ConversionResult>) {
        val entity = when (result) {
            is Either.Left -> task.copy(status = ConvertationTaskStatus.FAILED, failReason = result.toString())
            is Either.Right -> task.copy(status = ConvertationTaskStatus.SUCCESS)
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
                        output = emptyList(),
                        environmentId = it.environmentId
                    )
                },
                ifRight = {
                    ConvertationFinishedEvent(
                        userId = task.userId,
                        status = ConvertationFinishedStatus.SUCCESS,
                        log = it.log,
                        output = it.output,
                        environmentId = it.environmentId
                    )
                }
            )

        eventSender.sendEvent(event)
    }

    private fun processTask(task: ConvertationTask): Either<ConvertationError, ConversionResult> {
        val file = downloadService.download(task.sourceFileUrl)
            ?: return Either.Left(UnableFetchFile)

        return converterService.convertAndCollect(task.userId, file)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}