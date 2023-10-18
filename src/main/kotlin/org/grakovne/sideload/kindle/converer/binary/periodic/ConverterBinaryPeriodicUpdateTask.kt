package org.grakovne.sideload.kindle.converer.binary.periodic

import org.grakovne.sideload.kindle.converer.binary.update.ConverterBinaryUpdateService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ConverterBinaryPeriodicUpdateTask(
    private val converterBinaryUpdateService: ConverterBinaryUpdateService
) {

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    fun checkAndUpdateBinaries() = converterBinaryUpdateService.checkAndUpdate()
}