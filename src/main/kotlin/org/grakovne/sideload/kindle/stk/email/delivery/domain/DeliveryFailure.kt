package org.grakovne.sideload.kindle.stk.email.delivery.domain

import java.time.LocalDateTime
import java.util.UUID

data class DeliveryFailure(
    val id: UUID,
    val imapFolder: String,
    val imapUid: Long,
    val recipient: String?,
    val reason: FailureReason,
    val detail: String?,
    val detectedAt: LocalDateTime,
)
