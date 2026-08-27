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
            .onConflict(SHELF_REFERENCE.USER_ID)
            .doUpdate()
            .set(SHELF_REFERENCE.SHORT_ID, reference.shortId)
            .set(SHELF_REFERENCE.USER_ID, reference.userId)
            .execute()
        return reference
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
