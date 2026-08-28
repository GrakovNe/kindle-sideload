package org.grakovne.sideload.kindle.converter.binary.reference.repository

import org.grakovne.sideload.kindle.converter.binary.reference.domain.ConverterBinaryReference
import org.grakovne.sideload.kindle.generated.tables.ConverterBinaryReference.Companion.CONVERTER_BINARY_REFERENCE
import org.grakovne.sideload.kindle.generated.tables.records.ConverterBinaryReferenceRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class ConverterBinaryReferenceDao(
    private val dsl: DSLContext
) {

    fun save(reference: ConverterBinaryReference): ConverterBinaryReference {
        dsl.insertInto(CONVERTER_BINARY_REFERENCE)
            .set(CONVERTER_BINARY_REFERENCE.ID, reference.id)
            .set(CONVERTER_BINARY_REFERENCE.PUBLISHED_AT, toDb(reference.publishedAt))
            .onConflict(CONVERTER_BINARY_REFERENCE.ID)
            .doUpdate()
            .set(CONVERTER_BINARY_REFERENCE.PUBLISHED_AT, toDb(reference.publishedAt))
            .execute()
        return reference
    }

    fun findById(id: UUID): ConverterBinaryReference? =
        dsl.selectFrom(CONVERTER_BINARY_REFERENCE)
            .where(CONVERTER_BINARY_REFERENCE.ID.eq(id))
            .fetchOne()
            ?.let { it.toDomain() }

    // published_at is nullable (V2__create_converter_binary_table.sql) and PostgreSQL sorts NULLs
    // first under ORDER BY ... DESC, so such a row would win here and then fail to map to the
    // non-nullable domain field. "Latest" means latest known publication date, so skip them.
    fun findLatest(): ConverterBinaryReference? =
        dsl.selectFrom(CONVERTER_BINARY_REFERENCE)
            .where(CONVERTER_BINARY_REFERENCE.PUBLISHED_AT.isNotNull)
            .orderBy(CONVERTER_BINARY_REFERENCE.PUBLISHED_AT.desc())
            .limit(1)
            .fetchOne()
            ?.let { it.toDomain() }

    fun saveAll(references: List<ConverterBinaryReference>) = references.forEach { save(it) }

    fun findAll(): List<ConverterBinaryReference> =
        dsl.selectFrom(CONVERTER_BINARY_REFERENCE).fetch().map { it.toDomain() }

    fun count(): Int = dsl.fetchCount(CONVERTER_BINARY_REFERENCE)

    fun deleteAll() = dsl.deleteFrom(CONVERTER_BINARY_REFERENCE).execute()

    private fun toDb(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

    private fun fromDb(localDateTime: LocalDateTime): Instant =
        localDateTime.toInstant(ZoneOffset.UTC)

    private fun ConverterBinaryReferenceRecord.toDomain(): ConverterBinaryReference =
        ConverterBinaryReference(
            id = id!!,
            publishedAt = fromDb(publishedAt!!)
        )
}
