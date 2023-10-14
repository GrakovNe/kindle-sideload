package org.grakovne.sideload.kindle.converer.binary.update

import arrow.core.Either
import arrow.core.flatMap
import org.grakovne.sideload.kindle.converer.binary.fetch.GithubConverterBinaryFetchService
import org.grakovne.sideload.kindle.converer.binary.provider.ConverterBinaryProvider
import org.grakovne.sideload.kindle.converer.binary.reference.domain.BinaryError
import org.grakovne.sideload.kindle.converer.binary.reference.service.ConverterBinaryReferenceService
import org.grakovne.sideload.kindle.platform.PlatformService
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.time.Instant

@Service
class ConverterBinaryUpdateService(
    private val fetchService: GithubConverterBinaryFetchService,
    private val platformService: PlatformService,
    private val converterBinaryReferenceService: ConverterBinaryReferenceService,
    private val binaryProvider: ConverterBinaryProvider
) {

    fun checkAndUpdate() = when (checkIsBinariesPresented()) {
        true -> updateIfNewerVersion()
        false -> fetchUpdatedBinary()
    }
        .tap { converterBinaryReferenceService.updateLatestPublishedAt(it) }
        .map { }

    private fun checkIsBinariesPresented() =
        Files
            .list(binaryProvider.provideBinaryFolder().toPath())
            .toList()
            .isNotEmpty()

    private fun updateIfNewerVersion(): Either<BinaryError, Instant> {
        val latestPublished = fetchService
            .fetchLatestPublishedAt()
            .fold(
                ifLeft = { return Either.Left(it) },
                ifRight = { it }
            )

        val latestStored = converterBinaryReferenceService.fetchLatestPublishedAt() ?: Instant.MIN

        return when (latestPublished.isAfter(latestStored)) {
            true -> fetchUpdatedBinary()
            false -> Either.Left(BinaryError.NO_NEWEST_VERSIONS)
        }
    }

    private fun fetchUpdatedBinary(): Either<BinaryError, Instant> = platformService
        .fetchPlatformName()
        .mapLeft { BinaryError.UNABLE_TO_FETCH_BINARY_NO_REQUIRED_PLATFORM }
        .flatMap { fetchService.fetchForPlatform(it) }
}