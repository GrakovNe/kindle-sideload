package org.grakovne.sideload.kindle.converter.binary.reference.domain

import java.time.Instant
import java.util.UUID

data class ConverterBinaryReference(
    val id: UUID,
    val publishedAt: Instant?
)
