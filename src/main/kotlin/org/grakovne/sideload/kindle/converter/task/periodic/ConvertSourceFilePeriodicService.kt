package org.grakovne.sideload.kindle.converter.task.periodic

import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.FileDownloadService
import org.grakovne.sideload.kindle.common.parallelMap
import org.grakovne.sideload.kindle.converter.ConversionResult
import org.grakovne.sideload.kindle.converter.ConvertationError
import org.grakovne.sideload.kindle.converter.ConverterService
import org.grakovne.sideload.kindle.converter.FatalError
import org.grakovne.sideload.kindle.converter.UnableFetchFile
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.converter.task.service.ConvertationTaskService
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedEvent
import org.grakovne.sideload.kindle.events.internal.ConvertationFinishedStatus
import org.grakovne.sideload.kindle.stk.email.task.periodic.StkEmailPeriodicService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ConvertSourceFilePeriodicService(
    private val downloadService: FileDownloadService,
    private val converterService: ConverterService,
    private val taskService: ConvertationTaskService,
    private val eventSender: EventSender
) {

    @Scheduled(fixedDelay = 100)
    fun convertSourceFiles() {
        logger.trace { "Running periodically task ${this.javaClass.simpleName}" }

        runBlocking {
            taskService
                .fetchTasksForProcessing()
                .parallelMap(scope = CoroutineScope(Dispatchers.IO)) { it to processTask(it) }
                .map { (task, result) ->
                    notifyUser(task, result)
                    task to result
                }
                .map { (task, result) ->
                    updateStatus(task, result)
                }
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
                ifLeft = { it: ConvertationError ->
                    ConvertationFinishedEvent(
                        userId = task.userId,
                        status = ConvertationFinishedStatus.FAILED,
                        log = if (it is FatalError) "Fatal Error occurred on file processing. " else it.details ?: "",
                        output = emptyList(),
                        environmentId = it.environmentId,
                        failureReason = it
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

    private fun processTask(task: ConvertationTask): Either<ConvertationError, ConversionResult> = try {
        downloadService
            .download(task.sourceFileUrl)
            ?.let { converterService.convertAndCollect(task.userId, it) }
            ?: Either.Left(UnableFetchFile)
    } catch (ex: Exception) {
        logger.error("Fatal error occurred while file convert task: $ex")
        Either.Left(FatalError(ex.stackTraceToString()))
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}