package org.grakovne.sideload.kindle.shelf.repository

import org.grakovne.sideload.kindle.generated.tables.ShelfItem.Companion.SHELF_ITEM
import org.grakovne.sideload.kindle.generated.tables.records.ShelfItemRecord
import org.grakovne.sideload.kindle.shelf.domain.ShelfItem
import org.grakovne.sideload.kindle.shelf.domain.ShelfItemStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class ShelfItemDao(
    private val dsl: DSLContext
) {

    fun save(item: ShelfItem): ShelfItem {
        dsl.insertInto(SHELF_ITEM)
            .set(SHELF_ITEM.ID, item.id)
            .set(SHELF_ITEM.SHELF_ID, item.shelfId)
            .set(SHELF_ITEM.ENVIRONMENT_ID, item.environmentId)
            .set(SHELF_ITEM.CREATED_AT, toDb(item.createdAt))
            .set(SHELF_ITEM.STATUS, item.status.name)
            .onConflict(SHELF_ITEM.ID)
            .doUpdate()
            .set(SHELF_ITEM.SHELF_ID, item.shelfId)
            .set(SHELF_ITEM.ENVIRONMENT_ID, item.environmentId)
            .set(SHELF_ITEM.CREATED_AT, toDb(item.createdAt))
            .set(SHELF_ITEM.STATUS, item.status.name)
            .execute()
        return item
    }

    fun findById(id: UUID): ShelfItem? =
        dsl.selectFrom(SHELF_ITEM)
            .where(SHELF_ITEM.ID.eq(id))
            .fetchOne()
            ?.let { it.toDomain() }

    fun findByShelfIdAndStatus(shelfId: UUID, status: ShelfItemStatus): List<ShelfItem> {
        return dsl.selectFrom(SHELF_ITEM)
            .where(SHELF_ITEM.SHELF_ID.eq(shelfId))
            .and(SHELF_ITEM.STATUS.eq(status.name))
            .fetch()
            .map { it.toDomain() }
    }

    fun findByEnvironmentId(environmentId: String): ShelfItem? =
        dsl.selectFrom(SHELF_ITEM)
            .where(SHELF_ITEM.ENVIRONMENT_ID.eq(environmentId))
            .fetchOne()
            ?.let { it.toDomain() }

    fun saveAll(items: List<ShelfItem>) = items.forEach { save(it) }

    fun findAll(): List<ShelfItem> =
        dsl.selectFrom(SHELF_ITEM).fetch().map { it.toDomain() }

    fun count(): Int = dsl.fetchCount(SHELF_ITEM)

    fun deleteAll() = dsl.deleteFrom(SHELF_ITEM).execute()

    private fun toDb(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

    private fun fromDb(localDateTime: LocalDateTime): Instant =
        localDateTime.toInstant(ZoneOffset.UTC)

    private fun ShelfItemRecord.toDomain(): ShelfItem =
        ShelfItem(
            id = id!!,
            shelfId = shelfId!!,
            environmentId = environmentId!!,
            createdAt = fromDb(createdAt!!),
            status = ShelfItemStatus.valueOf(status!!)
        )
}
