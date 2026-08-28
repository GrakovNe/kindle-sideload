package org.grakovne.sideload.kindle.shelf.repository

import org.grakovne.sideload.kindle.generated.tables.ShelfReference.Companion.SHELF_REFERENCE
import org.grakovne.sideload.kindle.generated.tables.records.ShelfReferenceRecord
import org.grakovne.sideload.kindle.shelf.domain.ShelfReference
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class ShelfReferenceDao(
    private val dsl: DSLContext
) {

    fun save(reference: ShelfReference): ShelfReference {
        dsl.insertInto(SHELF_REFERENCE)
            .set(SHELF_REFERENCE.ID, reference.id)
            .set(SHELF_REFERENCE.SHORT_ID, reference.shortId)
            .set(SHELF_REFERENCE.USER_ID, reference.userId)
            .onConflict(SHELF_REFERENCE.ID)
            .doUpdate()
            .set(SHELF_REFERENCE.SHORT_ID, reference.shortId)
            .set(SHELF_REFERENCE.USER_ID, reference.userId)
            .execute()
        return reference
    }

    /**
     * Claims a shelf for a user without overwriting anything. Both `user_id` and `short_id` are
     * unique (V10__add_shelf_tables.sql), and the insert yields to either one, so this is safe to
     * race: the winner keeps its row and the loser observes it.
     *
     * Returns the reference that ended up in the database for [ShelfReference.userId] — the
     * inserted one, or the row a concurrent caller committed first. Returns null when the insert
     * was rejected because somebody else holds [ShelfReference.shortId]; the caller is expected to
     * retry with a freshly generated one.
     */
    fun saveIfAbsent(reference: ShelfReference): ShelfReference? {
        dsl.insertInto(SHELF_REFERENCE)
            .set(SHELF_REFERENCE.ID, reference.id)
            .set(SHELF_REFERENCE.SHORT_ID, reference.shortId)
            .set(SHELF_REFERENCE.USER_ID, reference.userId)
            .onConflictDoNothing()
            .execute()
        return findByUserId(reference.userId)
    }

    fun findById(id: UUID): ShelfReference? =
        dsl.selectFrom(SHELF_REFERENCE)
            .where(SHELF_REFERENCE.ID.eq(id))
            .fetchOne()
            ?.let { it.toDomain() }

    fun findByUserId(userId: String): ShelfReference? =
        dsl.selectFrom(SHELF_REFERENCE)
            .where(SHELF_REFERENCE.USER_ID.eq(userId))
            .fetchOne()
            ?.let { it.toDomain() }

    fun findByShortId(shortId: String): ShelfReference? =
        dsl.selectFrom(SHELF_REFERENCE)
            .where(SHELF_REFERENCE.SHORT_ID.eq(shortId))
            .fetchOne()
            ?.let { it.toDomain() }

    fun saveAll(references: List<ShelfReference>) = references.forEach { save(it) }

    fun findAll(): List<ShelfReference> =
        dsl.selectFrom(SHELF_REFERENCE).fetch().map { it.toDomain() }

    fun count(): Int = dsl.fetchCount(SHELF_REFERENCE)

    fun deleteAll() = dsl.deleteFrom(SHELF_REFERENCE).execute()

    private fun ShelfReferenceRecord.toDomain(): ShelfReference =
        ShelfReference(
            id = id!!,
            shortId = shortId!!,
            userId = userId!!
        )
}
