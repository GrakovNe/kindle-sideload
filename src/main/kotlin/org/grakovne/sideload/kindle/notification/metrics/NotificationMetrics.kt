package org.grakovne.sideload.kindle.notification.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.grakovne.sideload.kindle.notification.domain.NotificationKind
import org.grakovne.sideload.kindle.notification.domain.NotificationStatus
import org.springframework.stereotype.Component

@Component
class NotificationMetrics(
    private val meterRegistry: MeterRegistry,
) {

    fun countEnqueued(kind: NotificationKind, count: Int) {
        if (count > 0) {
            counter(kind, NotificationStatus.PENDING).increment(count.toDouble())
        }
    }

    fun countCancelled(kind: NotificationKind, count: Int) {
        if (count > 0) {
            counter(kind, NotificationStatus.CANCELLED).increment(count.toDouble())
        }
    }

    fun countOutcome(kind: NotificationKind, status: NotificationStatus) =
        counter(kind, status).increment()

    private fun counter(kind: NotificationKind, status: NotificationStatus) =
        meterRegistry.counter(
            "kindle.sideload.notifications",
            "kind",
            kind.name.lowercase(),
            "status",
            status.name.lowercase(),
        )
}
