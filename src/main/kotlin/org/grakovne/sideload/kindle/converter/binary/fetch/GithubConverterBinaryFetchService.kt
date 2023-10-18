package org.grakovne.sideload.kindle.converter.binary.fetch

import arrow.core.Either
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

    override fun fetchLatestPublishedAt() = restTemplate
        .getForEntity(sourceProperties.releasesUrl, GitHubRelease::class.java).body
        ?.publishedAt
        ?.let { Either.Right(it) }
        ?: Either.Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_CONTENT)


    override fun fetchForPlatform(
        platform: String
    ): Either<BinaryError, Instant> {
        val releases = restTemplate
            .getForEntity(sourceProperties.releasesUrl, GitHubRelease::class.java).body
            ?: return Either.Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_CONTENT)

        val downloadLink = releases
            .assets
            .filter { asset -> asset.browserDownloadUrl.endsWith(sourceProperties.extension) }
            .find { asset -> asset.browserDownloadUrl.contains(platform) }?.browserDownloadUrl
            ?: return Either.Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_REQUIRED_PLATFORM)

        return restTemplate
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
            ?.let { archivedBinaryUnpackService.unpack(it) }
            ?.map { releases.publishedAt }
            ?: return Either.Left(BinaryError.UNABLE_TO_STORE_BINARY)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}