package org.grakovne.sideload.kindle.converter.binary.reference.service

import mu.KotlinLogging
import org.grakovne.sideload.kindle.converter.binary.reference.domain.ConverterBinaryReference
import org.grakovne.sideload.kindle.converter.binary.reference.repository.ConverterBinaryReferenceRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ConverterBinaryReferenceService(
    private val repository: ConverterBinaryReferenceRepository
) {

    fun updateLatestPublishedAt(publishedAt: Instant) {
        val entity = repository
            .also { logger.debug { "Updating the date of latest update of converter Binary" } }
            .findLatest()
            ?.takeIf { it.publishedAt == publishedAt }
            ?: createEntity(publishedAt)

        repository
            .save(entity)
            .also { logger.debug { "Latest update of converter Binary has been updated to ${it.publishedAt}" } }
    }

    fun fetchLatestPublishedAt() = repository
        .also { logger.debug { "Fetching the date of latest update of converter Binary" } }
        .findLatest()
        ?.also { logger.debug { "Found that latest update date of converter Binary is ${it.publishedAt}" } }
        ?.publishedAt


    private fun createEntity(publishedAt: Instant) = ConverterBinaryReference(
        id = UUID.randomUUID(),
        publishedAt = publishedAt
    )

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}