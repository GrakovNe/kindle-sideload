package org.grakovne.sideload.kindle.stk.email.delivery.service

import mu.KotlinLogging
import org.grakovne.sideload.kindle.stk.email.delivery.configuration.DeliveryMonitorProperties
import org.grakovne.sideload.kindle.stk.email.delivery.domain.DeliveryFailure
import org.grakovne.sideload.kindle.stk.email.delivery.imap.MailboxReader
import org.grakovne.sideload.kindle.stk.email.delivery.imap.RawEmailMessage
import org.grakovne.sideload.kindle.stk.email.delivery.metrics.DeliveryFailureMetrics
import org.grakovne.sideload.kindle.stk.email.delivery.repository.DeliveryFailureDao
import org.springframework.stereotype.Service

@Service
class DeliveryFailureMonitorService(
    private val properties: DeliveryMonitorProperties,
    private val mailboxReader: MailboxReader,
    private val classifier: DeliveryFailureClassifier,
    private val deliveryFailureDao: DeliveryFailureDao,
    private val deliveryFailureMetrics: DeliveryFailureMetrics,
) {

    fun poll() {
        if (properties.enabled) {
            mailboxReader.fetchUnseen(::handle)
        }
    }

    private fun handle(message: RawEmailMessage): Boolean {
        classifier.classify(message)?.let(::record)
        return true
    }

    private fun record(failure: DeliveryFailure) {
        if (deliveryFailureDao.saveIfAbsent(failure)) {
            deliveryFailureMetrics.record(failure.reason)
            logger.info {
                "Recorded delivery failure uid=${failure.imapUid} in ${failure.imapFolder}: " +
                    "${failure.reason.key} (${failure.detail})"
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
