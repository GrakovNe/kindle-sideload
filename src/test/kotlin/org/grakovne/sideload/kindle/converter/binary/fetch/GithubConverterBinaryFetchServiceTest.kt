package org.grakovne.sideload.kindle.converter.binary.fetch

import arrow.core.Either
import org.grakovne.sideload.kindle.converter.binary.configuration.ConverterBinaryProperties
import org.grakovne.sideload.kindle.converter.binary.configuration.ConverterSourceProperties
import org.grakovne.sideload.kindle.converter.binary.provider.Asset
import org.grakovne.sideload.kindle.converter.binary.provider.ConverterBinaryProvider
import org.grakovne.sideload.kindle.converter.binary.provider.GitHubRelease
import org.grakovne.sideload.kindle.converter.binary.reference.domain.BinaryError
import org.grakovne.sideload.kindle.converter.binary.unpack.ArchivedBinaryUnpackService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseExtractor
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GithubConverterBinaryFetchServiceTest {

    @TempDir
    lateinit var tempDir: File

    private val restTemplate: RestTemplate = mock()
    private lateinit var binaryFolder: File
    private lateinit var unpackService: ArchivedBinaryUnpackService
    private lateinit var sut: GithubConverterBinaryFetchService

    @BeforeEach
    fun setUp() {
        binaryFolder = File(tempDir, "binaries")
        unpackService = ArchivedBinaryUnpackService(
            ConverterBinaryProvider(
                ConverterBinaryProperties().apply {
                    binaryPersistencePath = binaryFolder.absolutePath
                    converterFileName = "fb2c"
                }
            )
        )
        val properties = ConverterSourceProperties().apply { releasesUrl = "https://github.com/releases" }
        sut = GithubConverterBinaryFetchService(restTemplate, properties, unpackService)
    }

    @Test
    fun `reports the latest published date`() {
        stubReleases(release(Instant.parse("2026-08-01T00:00:00Z")))

        val result = sut.fetchLatestPublishedAt()

        assertEquals(Either.Right(Instant.parse("2026-08-01T00:00:00Z")), result)
    }

    @Test
    fun `reports no content when the release response has no body`() {
        whenever(restTemplate.getForEntity(any<String>(), eq(GitHubRelease::class.java)))
            .thenReturn(ResponseEntity<GitHubRelease>(HttpStatus.NO_CONTENT))

        val result = sut.fetchLatestPublishedAt()

        assertEquals(Either.Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_CONTENT), result)
    }

    @Test
    fun `downloads and unpacks the binary for the required platform`() {
        stubReleases(
            release(
                Instant.parse("2026-08-01T00:00:00Z"),
                listOf(
                    Asset("https://github.com/release/darwin-arm64.zip"),
                    Asset("https://github.com/release/linux-x64.zip")
                )
            )
        )
        stubBinaryDownload(buildZip())

        val result = sut.fetchForPlatform("darwin-arm64")

        assertEquals(Either.Right(Instant.parse("2026-08-01T00:00:00Z")), result)
        assertTrue(File(binaryFolder, "fb2c").readText() == "fake converter binary")
    }

    @Test
    fun `reports no required platform when no asset matches`() {
        stubReleases(
            release(
                Instant.parse("2026-08-01T00:00:00Z"),
                listOf(Asset("https://github.com/release/linux-x64.zip"))
            )
        )

        val result = sut.fetchForPlatform("darwin-arm64")

        assertEquals(Either.Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_REQUIRED_PLATFORM), result)
    }

    @Test
    fun `reports no content when the release body is missing on platform fetch`() {
        whenever(restTemplate.getForEntity(any<String>(), eq(GitHubRelease::class.java)))
            .thenReturn(ResponseEntity<GitHubRelease>(HttpStatus.NO_CONTENT))

        val result = sut.fetchForPlatform("darwin-arm64")

        assertEquals(Either.Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_CONTENT), result)
    }

    private fun stubReleases(release: GitHubRelease) {
        whenever(restTemplate.getForEntity(any<String>(), eq(GitHubRelease::class.java)))
            .thenReturn(ResponseEntity.ok(release))
    }

    private fun stubBinaryDownload(archive: File) {
        val response: ClientHttpResponse = mock()
        whenever(response.body).thenReturn(ByteArrayInputStream(archive.readBytes()))
        whenever(restTemplate.execute(any<String>(), any<HttpMethod>(), isNull(), any<ResponseExtractor<*>>()))
            .thenAnswer { invocation ->
                val extractor = invocation.getArgument(3, ResponseExtractor::class.java)
                (extractor as ResponseExtractor<ClientHttpResponse>).extractData(response)
            }
    }

    private fun release(publishedAt: Instant, assets: List<Asset> = emptyList()) =
        GitHubRelease(publishedAt = publishedAt, assets = assets)

    private fun buildZip(): File {
        val file = File(tempDir, "source.zip")
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.putNextEntry(ZipEntry("fb2c"))
            zip.write("fake converter binary".toByteArray())
            zip.closeEntry()
        }
        return file
    }
}
