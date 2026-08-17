package org.grakovne.sideload.kindle.converter.binary.reference.service

import org.grakovne.sideload.kindle.converter.binary.reference.domain.ConverterBinaryReference
import org.grakovne.sideload.kindle.converter.binary.reference.repository.ConverterBinaryReferenceRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConverterBinaryReferenceServiceTest {

    private val repository: ConverterBinaryReferenceRepository = mock()
    private val sut = ConverterBinaryReferenceService(repository)

    @Test
    fun `returns the published date of the latest reference`() {
        whenever(repository.findLatest()).thenReturn(reference(Instant.parse("2026-08-01T00:00:00Z")))

        val result = sut.fetchLatestPublishedAt()

        assertEquals(Instant.parse("2026-08-01T00:00:00Z"), result)
    }

    @Test
    fun `returns null when there is no reference yet`() {
        whenever(repository.findLatest()).thenReturn(null)

        assertNull(sut.fetchLatestPublishedAt())
    }

    @Test
    fun `stores the latest published date as a new reference`() {
        whenever(repository.findLatest()).thenReturn(null)
        whenever(repository.save(any<ConverterBinaryReference>()))
            .thenAnswer { it.arguments.first() as ConverterBinaryReference }

        sut.updateLatestPublishedAt(Instant.parse("2026-08-01T00:00:00Z"))

        val captor = argumentCaptor<ConverterBinaryReference>()
        verify(repository).save(captor.capture())
        assertEquals(Instant.parse("2026-08-01T00:00:00Z"), captor.firstValue.publishedAt)
    }

    @Test
    fun `reuses the existing reference when the date is the same`() {
        val existing = reference(Instant.parse("2026-08-01T00:00:00Z"))
        whenever(repository.findLatest()).thenReturn(existing)
        whenever(repository.save(any<ConverterBinaryReference>()))
            .thenAnswer { it.arguments.first() as ConverterBinaryReference }

        sut.updateLatestPublishedAt(Instant.parse("2026-08-01T00:00:00Z"))

        verify(repository).save(existing)
    }

    @Test
    fun `replaces the existing reference when the date is newer`() {
        whenever(repository.findLatest()).thenReturn(reference(Instant.parse("2026-07-01T00:00:00Z")))
        whenever(repository.save(any<ConverterBinaryReference>()))
            .thenAnswer { it.arguments.first() as ConverterBinaryReference }

        sut.updateLatestPublishedAt(Instant.parse("2026-08-01T00:00:00Z"))

        val captor = argumentCaptor<ConverterBinaryReference>()
        verify(repository).save(captor.capture())
        assertEquals(Instant.parse("2026-08-01T00:00:00Z"), captor.firstValue.publishedAt)
    }

    private fun reference(publishedAt: Instant) = ConverterBinaryReference(
        id = UUID.randomUUID(),
        publishedAt = publishedAt
    )
}
