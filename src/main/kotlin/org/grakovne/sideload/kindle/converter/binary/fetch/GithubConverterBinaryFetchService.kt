package org.grakovne.sideload.kindle.converter.binary.fetch

import arrow.core.Either
import arrow.core.flatMap
import mu.KotlinLogging
import org.grakovne.sideload.kindle.converter.binary.configuration.ConverterBinarySourceProperties
import org.grakovne.sideload.kindle.converter.binary.provider.GitHubRelease
import org.grakovne.sideload.kindle.converter.binary.reference.domain.BinaryError
import org.grakovne.sideload.kindle.converter.binary.unpack.ArchivedBinaryUnpackService
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream
import java.time.Instant


@Service
class GithubConverterBinaryFetchService(
    private val restTemplate: RestTemplate,
    private val sourceProperties: ConverterBinarySourceProperties,
    private val archivedBinaryUnpackService: ArchivedBinaryUnpackService
) : ConverterBinaryFetchService {

    private fun fetchReleases() = restTemplate
        .also { logger.info { "Checking newest versions of Binary at GitHub" } }
        .getForEntity(sourceProperties.releasesUrl, GitHubRelease::class.java).body
        ?.let { release ->
            logger
                .info { "Newest versions of Binary checked. Latest version has been published at: ${release.publishedAt}" }
            let { Either.Right(release) }
        }
        ?: Either
            .Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_CONTENT)
            .also { logger.error { "Unable to check newest versions of Binary. See details: $it" } }

    override fun fetchLatestPublishedAt() = fetchReleases().map { it.publishedAt }

    override fun fetchForPlatform(
        platform: String
    ): Either<BinaryError, Instant> {
        val releases = fetchReleases()

        val downloadLink = releases
            .tap { logger.info { "Fetching latest version of Binary for $platform" } }
            .map { it.assets }
            .map { it.filter { asset -> asset.browserDownloadUrl.endsWith(sourceProperties.extension) } }
            .map {
                it
                    .find { asset -> asset.browserDownloadUrl.contains(platform) }
                    ?.browserDownloadUrl
                    ?: return Either
                        .Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_REQUIRED_PLATFORM)
                        .also { logger.error { "Unable to find Binary for $platform, failing" } }
            }
            .fold(
                ifLeft = { return Either.Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_CONTENT) },
                ifRight = { it }
            )

        return restTemplate
            .also { logger.info { "Fetching Binary file by url: $downloadLink" } }
            .execute(
                downloadLink,
                HttpMethod.GET,
                null,
                {
                    val file = File.createTempFile("kindle_sideload_binary", sourceProperties.extension)
                    StreamUtils.copy(it.body, FileOutputStream(file))
                    file
                }
            )
            ?.also { logger.info { "Fetched Binary file by url: $downloadLink" } }
            ?.let { archivedBinaryUnpackService.unpack(it) }
            ?.tap { logger.info { "Saved unpacked Binary file" } }
            ?.flatMap { releases.map { it.publishedAt } }
            ?: return Either
                .Left(BinaryError.UNABLE_TO_STORE_BINARY)
                .also { logger.error { "Unable to store Binary locally. See details: $it" } }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}