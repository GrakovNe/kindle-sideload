package org.grakovne.sideload.kindle.converer.binary.reference.service

import org.grakovne.sideload.kindle.converer.binary.reference.domain.ConverterBinaryReference
import org.grakovne.sideload.kindle.converer.binary.reference.repository.ConverterBinaryReferenceRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class ConverterBinaryReferenceService(
    private val repository: ConverterBinaryReferenceRepository
) {

    fun updateLatestPublishedAt(publishedAt: Instant) {
        val entity = repository
            .findLatest()
            ?.takeIf { it.publishedAt == publishedAt }
            ?: createEntity(publishedAt)

        repository.save(entity)
    }

    fun fetchLatestPublishedAt() = repository.findLatest()?.publishedAt

    private fun createEntity(publishedAt: Instant) = ConverterBinaryReference(
        id = UUID.randomUUID(),
        publishedAt = publishedAt
    )
}