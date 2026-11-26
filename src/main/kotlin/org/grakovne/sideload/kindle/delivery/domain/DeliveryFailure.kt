package org.grakovne.sideload.kindle.delivery.domain

import java.time.LocalDateTime
import java.util.UUID

data class DeliveryFailure(
    val id: UUID,
    val imapFolder: String,
    val imapUid: Long,
    val recipient: String?,
    val reason: DeliveryFailureReason,
    val detail: String?,
    val detectedAt: LocalDateTime,
)
