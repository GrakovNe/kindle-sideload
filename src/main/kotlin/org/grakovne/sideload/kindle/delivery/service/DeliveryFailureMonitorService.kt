package org.grakovne.sideload.kindle.delivery.service

import mu.KotlinLogging
import org.grakovne.sideload.kindle.delivery.configuration.DeliveryMonitorProperties
import org.grakovne.sideload.kindle.delivery.imap.MailboxReader
import org.grakovne.sideload.kindle.delivery.metrics.DeliveryFailureMetrics
import org.grakovne.sideload.kindle.delivery.repository.DeliveryFailureDao
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
        if (!properties.enabled) {
            return
        }

        mailboxReader.fetchUnseen { message ->
            val failure = classifier.classify(message)
                ?: return@fetchUnseen true

            if (deliveryFailureDao.saveIfAbsent(failure)) {
                deliveryFailureMetrics.record(failure.reason)
                logger.info(
                    "Recorded delivery failure uid=${failure.imapUid} in ${failure.imapFolder}: " +
                        "${failure.reason} (${failure.detail})"
                )
            }

            true
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
