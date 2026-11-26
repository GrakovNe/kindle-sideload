package org.grakovne.sideload.kindle.stk.email.delivery.periodic

import org.grakovne.sideload.kindle.stk.email.delivery.service.DeliveryFailureMonitorService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DeliveryFailurePeriodicServiceTest {

    private val monitorService = mock<DeliveryFailureMonitorService>()
    private val periodicService = DeliveryFailurePeriodicService(monitorService)

    @Test
    fun `delegates poll to monitor service`() {
        periodicService.pollMailbox()

        verify(monitorService).poll()
    }

    @Test
    fun `swallows poll failures`() {
        doThrow(RuntimeException("imap exploded")).whenever(monitorService).poll()

        periodicService.pollMailbox()

        verify(monitorService).poll()
    }

    @Test
    fun `keeps polling after a failure`() {
        whenever(monitorService.poll())
            .thenThrow(RuntimeException("first poll failed"))
            .thenAnswer { }

        periodicService.pollMailbox()
        periodicService.pollMailbox()

        verify(monitorService, times(2)).poll()
    }
}
