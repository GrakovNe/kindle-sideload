package org.grakovne.sideload.kindle.converter.binary.reference.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Instant
import java.util.*

@Entity
data class ConverterBinaryReference(
    @Id
    val id: UUID,
    val publishedAt: Instant
)