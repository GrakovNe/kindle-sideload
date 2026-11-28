package org.grakovne.sideload.kindle.notification.repository

import org.grakovne.sideload.kindle.generated.tables.SenderAddressAnnouncement.Companion.SENDER_ADDRESS_ANNOUNCEMENT
import org.grakovne.sideload.kindle.generated.tables.records.SenderAddressAnnouncementRecord
import org.grakovne.sideload.kindle.notification.domain.SenderAddressAnnouncement
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class SenderAddressAnnouncementDao(
    private val dsl: DSLContext
) {

    fun find(): SenderAddressAnnouncement? =
        dsl.selectFrom(SENDER_ADDRESS_ANNOUNCEMENT)
            .where(SENDER_ADDRESS_ANNOUNCEMENT.ID.eq(SenderAddressAnnouncement.SINGLETON_ID))
            .fetchOne()
            ?.toDomain()

    fun insertIfAbsent(address: String, generation: Int, announcedAt: Instant): Boolean =
        dsl.insertInto(SENDER_ADDRESS_ANNOUNCEMENT)
            .set(SENDER_ADDRESS_ANNOUNCEMENT.ID, SenderAddressAnnouncement.SINGLETON_ID)
            .set(SENDER_ADDRESS_ANNOUNCEMENT.ADDRESS, address)
            .set(SENDER_ADDRESS_ANNOUNCEMENT.GENERATION, generation)
            .set(SENDER_ADDRESS_ANNOUNCEMENT.ANNOUNCED_AT, toDb(announcedAt))
            .onConflictDoNothing()
            .execute() > 0

    /**
     * Moves the announcement to [newAddress] only while it still holds [expectedAddress], so two
     * overlapping startups cannot both decide they are the one that noticed the change.
     */
    fun claimAddressChange(
        expectedAddress: String?,
        newAddress: String,
        generation: Int,
        announcedAt: Instant,
    ): Boolean {
        val storedAddress = SENDER_ADDRESS_ANNOUNCEMENT.ADDRESS
        val addressMatches = when (expectedAddress) {
            null -> storedAddress.isNull
            else -> storedAddress.eq(expectedAddress)
        }

        return dsl.update(SENDER_ADDRESS_ANNOUNCEMENT)
            .set(SENDER_ADDRESS_ANNOUNCEMENT.ADDRESS, newAddress)
            .set(SENDER_ADDRESS_ANNOUNCEMENT.GENERATION, generation)
            .set(SENDER_ADDRESS_ANNOUNCEMENT.ANNOUNCED_AT, toDb(announcedAt))
            .where(SENDER_ADDRESS_ANNOUNCEMENT.ID.eq(SenderAddressAnnouncement.SINGLETON_ID))
            .and(addressMatches)
            .execute() > 0
    }

    fun bumpGeneration(address: String, generation: Int, announcedAt: Instant): Boolean =
        dsl.update(SENDER_ADDRESS_ANNOUNCEMENT)
            .set(SENDER_ADDRESS_ANNOUNCEMENT.ADDRESS, address)
            .set(SENDER_ADDRESS_ANNOUNCEMENT.GENERATION, generation)
            .set(SENDER_ADDRESS_ANNOUNCEMENT.ANNOUNCED_AT, toDb(announcedAt))
            .where(SENDER_ADDRESS_ANNOUNCEMENT.ID.eq(SenderAddressAnnouncement.SINGLETON_ID))
            .execute() > 0

    fun deleteAll() = dsl.deleteFrom(SENDER_ADDRESS_ANNOUNCEMENT).execute()

    private fun toDb(instant: Instant?): LocalDateTime? =
        instant?.let { LocalDateTime.ofInstant(it, ZoneOffset.UTC) }

    private fun fromDb(localDateTime: LocalDateTime?): Instant? =
        localDateTime?.toInstant(ZoneOffset.UTC)

    private fun SenderAddressAnnouncementRecord.toDomain(): SenderAddressAnnouncement =
        SenderAddressAnnouncement(
            id = id!!,
            address = address,
            generation = generation ?: 0,
            announcedAt = fromDb(announcedAt),
        )
}
