package org.grakovne.sideload.kindle.stk.email.delivery.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.grakovne.sideload.kindle.stk.email.delivery.domain.FailureReason
import org.springframework.stereotype.Component

@Component
class DeliveryFailureMetrics(
    private val meterRegistry: MeterRegistry
) {

    fun record(reason: FailureReason) {
        meterRegistry.counter(FAILURE_METRIC, listOf(Tag.of("reason", reason.key)))
            .increment()
    }

    companion object {
        const val FAILURE_METRIC = "kindle.sideload.delivery.failures"
    }
}
