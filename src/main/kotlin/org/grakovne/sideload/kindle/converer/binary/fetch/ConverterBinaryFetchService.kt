package org.grakovne.sideload.kindle.converer.binary.fetch

import arrow.core.Either
import org.grakovne.sideload.kindle.converer.binary.reference.domain.BinaryError
import java.io.File
import java.time.Instant

interface ConverterBinaryFetchService {

    fun fetchLatestPublishedAt(): Either<BinaryError, Instant>

    fun fetchForPlatform(platform: String): Either<BinaryError, Instant>
}