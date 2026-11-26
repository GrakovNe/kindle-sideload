package org.grakovne.sideload.kindle.stk.email.delivery.repository

import org.grakovne.sideload.kindle.generated.tables.DeliveryFailure.Companion.DELIVERY_FAILURE
import org.grakovne.sideload.kindle.generated.tables.records.DeliveryFailureRecord
import org.grakovne.sideload.kindle.stk.email.delivery.domain.DeliveryFailure
import org.grakovne.sideload.kindle.stk.email.delivery.domain.FailureReason
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class DeliveryFailureDao(
    private val dsl: DSLContext
) {

    fun saveIfAbsent(failure: DeliveryFailure): Boolean =
        dsl.insertInto(DELIVERY_FAILURE)
            .set(DELIVERY_FAILURE.ID, failure.id)
            .set(DELIVERY_FAILURE.IMAP_FOLDER, failure.imapFolder)
            .set(DELIVERY_FAILURE.IMAP_UID, failure.imapUid)
            .set(DELIVERY_FAILURE.RECIPIENT, failure.recipient)
            .set(DELIVERY_FAILURE.REASON, failure.reason.key)
            .set(DELIVERY_FAILURE.DETAIL, failure.detail)
            .set(DELIVERY_FAILURE.DETECTED_AT, failure.detectedAt)
            .onConflict(DELIVERY_FAILURE.IMAP_FOLDER, DELIVERY_FAILURE.IMAP_UID)
            .doNothing()
            .execute() > 0

    fun findAll(): List<DeliveryFailure> =
        dsl.selectFrom(DELIVERY_FAILURE)
            .orderBy(DELIVERY_FAILURE.DETECTED_AT.desc())
            .fetch()
            .map { it.toDomain() }

    private fun DeliveryFailureRecord.toDomain(): DeliveryFailure =
        DeliveryFailure(
            id = id!!,
            imapFolder = imapFolder!!,
            imapUid = imapUid!!,
            recipient = recipient,
            reason = FailureReason(reason!!),
            detail = detail,
            detectedAt = detectedAt!!,
        )
}
