package org.grakovne.sideload.kindle.converter.binary.fetch

import arrow.core.Either
import org.grakovne.sideload.kindle.converter.binary.reference.domain.BinaryError
import java.time.Instant

interface ConverterBinaryFetchService {

    fun fetchLatestPublishedAt(): Either<BinaryError, Instant>

    fun fetchForPlatform(platform: String): Either<BinaryError, Instant>
}