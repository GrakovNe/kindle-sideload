package org.grakovne.sideload.kindle.converter.binary.periodic

import mu.KotlinLogging
import org.grakovne.sideload.kindle.converter.binary.update.ConverterBinaryUpdateService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ConverterBinaryPeriodicUpdateTask(
    private val converterBinaryUpdateService: ConverterBinaryUpdateService
) {

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    fun checkAndUpdateBinaries() = converterBinaryUpdateService
        .also { logger.info { "Running periodically task ${this.javaClass.simpleName}" } }
        .checkAndUpdate()

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}