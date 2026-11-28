package org.grakovne.sideload.kindle.notification.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.grakovne.sideload.kindle.notification.domain.NotificationKind
import org.grakovne.sideload.kindle.notification.domain.NotificationStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NotificationMetricsTest {

    private val meterRegistry = SimpleMeterRegistry()
    private val metrics = NotificationMetrics(meterRegistry)

    @Test
    fun `exposes the campaign progress tagged by kind and status`() {
        metrics.countEnqueued(NotificationKind.SENDER_ADDRESS_CHANGED, 10)
        metrics.countOutcome(NotificationKind.SENDER_ADDRESS_CHANGED, NotificationStatus.SENT)
        metrics.countOutcome(NotificationKind.SENDER_ADDRESS_CHANGED, NotificationStatus.SENT)
        metrics.countOutcome(NotificationKind.SENDER_ADDRESS_CHANGED, NotificationStatus.UNDELIVERABLE)

        assertEquals(10.0, count(NotificationStatus.PENDING))
        assertEquals(2.0, count(NotificationStatus.SENT))
        assertEquals(1.0, count(NotificationStatus.UNDELIVERABLE))
        assertEquals(0.0, count(NotificationStatus.FAILED))
    }

    @Test
    fun `does not emit a series for a no-op batch`() {
        metrics.countEnqueued(NotificationKind.SENDER_ADDRESS_CHANGED, 0)
        metrics.countCancelled(NotificationKind.SENDER_ADDRESS_CHANGED, 0)

        assertEquals(0.0, count(NotificationStatus.PENDING))
        assertEquals(0.0, count(NotificationStatus.CANCELLED))
    }

    @Test
    fun `counts the slots cancelled by a superseded campaign`() {
        metrics.countCancelled(NotificationKind.SENDER_ADDRESS_CHANGED, 4)

        assertEquals(4.0, count(NotificationStatus.CANCELLED))
    }

    private fun count(status: NotificationStatus): Double =
        meterRegistry.find(METRIC)
            .tag("kind", "sender_address_changed")
            .tag("status", status.name.lowercase())
            .counter()
            ?.count() ?: 0.0

    companion object {
        private const val METRIC = "kindle.sideload.notifications"
    }
}
