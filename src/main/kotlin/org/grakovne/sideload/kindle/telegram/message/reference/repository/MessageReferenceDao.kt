package org.grakovne.sideload.kindle.telegram.message.reference.repository

import org.grakovne.sideload.kindle.generated.tables.MessageReference.Companion.MESSAGE_REFERENCE
import org.grakovne.sideload.kindle.generated.tables.records.MessageReferenceRecord
import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageReference
import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class MessageReferenceDao(
    private val dsl: DSLContext
) {

    fun save(reference: MessageReference): MessageReference {
        dsl.insertInto(MESSAGE_REFERENCE)
            .set(MESSAGE_REFERENCE.ID, reference.id)
            .set(MESSAGE_REFERENCE.STATUS, reference.status.name)
            .onConflict(MESSAGE_REFERENCE.ID)
            .doUpdate()
            .set(MESSAGE_REFERENCE.STATUS, reference.status.name)
            .execute()
        return reference
    }

    fun findById(id: String): MessageReference? =
        dsl.selectFrom(MESSAGE_REFERENCE)
            .where(MESSAGE_REFERENCE.ID.eq(id))
            .fetchOne()
            ?.let { it.toDomain() }

    fun saveAll(references: List<MessageReference>) = references.forEach { save(it) }

    fun findAll(): List<MessageReference> =
        dsl.selectFrom(MESSAGE_REFERENCE).fetch().map { it.toDomain() }

    fun count(): Int = dsl.fetchCount(MESSAGE_REFERENCE)

    fun deleteAll() = dsl.deleteFrom(MESSAGE_REFERENCE).execute()

    private fun MessageReferenceRecord.toDomain(): MessageReference {
        val id = requireNotNull(this.id) { "MessageReference.id must be set by the database" }
        val status = requireNotNull(this.status) { "MessageReference.status must be set by the database" }

        return MessageReference(
            id = id,
            status = MessageStatus.valueOf(status)
        )
    }
}
