package org.grakovne.sideload.kindle.stk.email.delivery.periodic

import mu.KotlinLogging
import org.grakovne.sideload.kindle.stk.email.delivery.service.DeliveryFailureMonitorService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DeliveryFailurePeriodicService(
    private val deliveryFailureMonitorService: DeliveryFailureMonitorService
) {

    @Scheduled(fixedDelay = POLL_INTERVAL, initialDelay = INITIAL_DELAY)
    fun pollMailbox() {
        runCatching(deliveryFailureMonitorService::poll)
            .onFailure { logger.warn(it) { "Delivery failure mailbox poll failed" } }
    }

    companion object {
        private const val POLL_INTERVAL = 5 * 60 * 1000L
        private const val INITIAL_DELAY = 60 * 1000L
        private val logger = KotlinLogging.logger {}
    }
}
