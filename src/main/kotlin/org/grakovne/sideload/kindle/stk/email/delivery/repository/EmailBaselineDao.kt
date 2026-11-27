package org.grakovne.sideload.kindle.stk.email.delivery.repository

import org.grakovne.sideload.kindle.generated.tables.EmailBaseline.Companion.EMAIL_BASELINE
import org.grakovne.sideload.kindle.generated.tables.records.EmailBaselineRecord
import org.grakovne.sideload.kindle.stk.email.delivery.domain.EmailBaseline
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class EmailBaselineDao(
    private val dsl: DSLContext
) {

    fun find(imapFolder: String): EmailBaseline? =
        dsl.selectFrom(EMAIL_BASELINE)
            .where(EMAIL_BASELINE.IMAP_FOLDER.eq(imapFolder))
            .fetchOne()
            ?.toDomain()

    fun save(baseline: EmailBaseline): Int =
        dsl.insertInto(EMAIL_BASELINE)
            .set(EMAIL_BASELINE.IMAP_FOLDER, baseline.imapFolder)
            .set(EMAIL_BASELINE.UID_VALIDITY, baseline.uidValidity)
            .set(EMAIL_BASELINE.LAST_PROCESSED_UID, baseline.lastProcessedUid)
            .set(EMAIL_BASELINE.UPDATED_AT, baseline.updatedAt)
            .onConflict(EMAIL_BASELINE.IMAP_FOLDER)
            .doUpdate()
            .set(EMAIL_BASELINE.UID_VALIDITY, baseline.uidValidity)
            .set(EMAIL_BASELINE.LAST_PROCESSED_UID, baseline.lastProcessedUid)
            .set(EMAIL_BASELINE.UPDATED_AT, baseline.updatedAt)
            .execute()

    private fun EmailBaselineRecord.toDomain(): EmailBaseline =
        EmailBaseline(
            imapFolder = imapFolder!!,
            uidValidity = uidValidity!!,
            lastProcessedUid = lastProcessedUid!!,
            updatedAt = updatedAt!!,
        )
}
