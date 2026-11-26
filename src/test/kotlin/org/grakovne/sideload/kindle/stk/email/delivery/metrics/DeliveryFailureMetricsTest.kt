package org.grakovne.sideload.kindle.stk.email.delivery.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.grakovne.sideload.kindle.stk.email.delivery.domain.FailureReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeliveryFailureMetricsTest {

    private val meterRegistry = SimpleMeterRegistry()
    private val metrics = DeliveryFailureMetrics(meterRegistry)

    @Test
    fun `increments counter tagged by reason`() {
        metrics.record(FailureReason("throttled"))

        assertEquals(1.0, count("throttled"))
    }

    @Test
    fun `accumulates repeated failures of the same reason`() {
        listOf("throttled", "throttled", "throttled").forEach { metrics.record(FailureReason(it)) }

        assertEquals(3.0, count("throttled"))
    }

    @Test
    fun `keeps separate series per reason`() {
        metrics.record(FailureReason("throttled"))
        metrics.record(FailureReason("malicious-content"))
        metrics.record(FailureReason("5.7.234"))

        assertEquals(1.0, count("throttled"))
        assertEquals(1.0, count("malicious-content"))
        assertEquals(1.0, count("5.7.234"))
    }

    private fun count(reason: String): Double? =
        meterRegistry.find(DeliveryFailureMetrics.FAILURE_METRIC)
            .tag("reason", reason)
            .counter()
            ?.count()
}
