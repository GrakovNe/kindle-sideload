package org.grakovne.sideload.kindle.stk.email.delivery.service

import mu.KotlinLogging
import org.grakovne.sideload.kindle.stk.email.delivery.configuration.DeliveryMonitorProperties
import org.grakovne.sideload.kindle.stk.email.delivery.domain.DeliveryFailure
import org.grakovne.sideload.kindle.stk.email.delivery.domain.EmailBaseline
import org.grakovne.sideload.kindle.stk.email.delivery.imap.MailboxReader
import org.grakovne.sideload.kindle.stk.email.delivery.imap.RawEmailMessage
import org.grakovne.sideload.kindle.stk.email.delivery.imap.ScanBaseline
import org.grakovne.sideload.kindle.stk.email.delivery.imap.ScanResult
import org.grakovne.sideload.kindle.stk.email.delivery.metrics.DeliveryFailureMetrics
import org.grakovne.sideload.kindle.stk.email.delivery.repository.DeliveryFailureDao
import org.grakovne.sideload.kindle.stk.email.delivery.repository.EmailBaselineDao
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Reads the sender mailbox from a locally stored baseline instead of the IMAP `\Seen`
 * flag: the monitor must not mark anything as read, and what the user happens to leave
 * unread in the mailbox has nothing to do with what the monitor has already processed.
 */
@Service
class DeliveryFailureMonitorService(
    private val properties: DeliveryMonitorProperties,
    private val mailboxReader: MailboxReader,
    private val classifier: DeliveryFailureClassifier,
    private val deliveryFailureDao: DeliveryFailureDao,
    private val emailBaselineDao: EmailBaselineDao,
    private val deliveryFailureMetrics: DeliveryFailureMetrics,
) {

    fun poll() {
        if (!properties.enabled) return

        val baseline = emailBaselineDao.find(properties.folder)?.let { ScanBaseline(it.uidValidity, it.lastProcessedUid) }
        val result = mailboxReader.scan(baseline, ::handle)
        advance(baseline, result)
    }

    private fun handle(message: RawEmailMessage) {
        classifier.classify(message)?.let(::record)
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

    /**
     * Within one uid epoch the baseline only ever moves forward; a new epoch is a
     * recreated folder, so its position replaces the stale one.
     */
    private fun advance(baseline: ScanBaseline?, result: ScanResult) {
        val stored = baseline?.takeIf { it.uidValidity == result.uidValidity }?.lastProcessedUid ?: 0L
        if (result.lastHandledUid <= stored) return

        emailBaselineDao.save(
            EmailBaseline(
                imapFolder = result.folder,
                uidValidity = result.uidValidity,
                lastProcessedUid = result.lastHandledUid,
                updatedAt = LocalDateTime.now(),
            ),
        )
        logger.info { "Baseline for ${result.folder} advanced to uid ${result.lastHandledUid}" }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
