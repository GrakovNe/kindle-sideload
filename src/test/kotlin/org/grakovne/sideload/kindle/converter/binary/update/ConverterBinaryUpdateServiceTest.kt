package org.grakovne.sideload.kindle.converter.binary.update

import arrow.core.Either
import org.grakovne.sideload.kindle.common.platform.PlatformError
import org.grakovne.sideload.kindle.common.platform.PlatformService
import org.grakovne.sideload.kindle.converter.binary.configuration.ConverterBinaryProperties
import org.grakovne.sideload.kindle.converter.binary.fetch.GithubConverterBinaryFetchService
import org.grakovne.sideload.kindle.converter.binary.provider.ConverterBinaryProvider
import org.grakovne.sideload.kindle.converter.binary.reference.domain.BinaryError
import org.grakovne.sideload.kindle.converter.binary.reference.service.ConverterBinaryReferenceService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals

class ConverterBinaryUpdateServiceTest {

    @TempDir
    lateinit var tempDir: File

    private val fetchService: GithubConverterBinaryFetchService = mock()
    private val platformService: PlatformService = mock()
    private val referenceService: ConverterBinaryReferenceService = mock()
    private lateinit var binaryProvider: ConverterBinaryProvider
    private lateinit var sut: ConverterBinaryUpdateService

    @BeforeEach
    fun setUp() {
        binaryProvider = ConverterBinaryProvider(
            ConverterBinaryProperties().apply {
                binaryPersistencePath = File(tempDir, "binaries").absolutePath
                converterFileName = "fb2c"
            }
        )
        sut = ConverterBinaryUpdateService(fetchService, platformService, referenceService, binaryProvider)
    }

    @Test
    fun `fetches the binary for the current platform when no local binary exists`() {
        whenever(platformService.fetchPlatformName()).thenReturn(Either.Right("darwin-arm64"))
        whenever(fetchService.fetchForPlatform("darwin-arm64"))
            .thenReturn(Either.Right(Instant.parse("2026-08-01T00:00:00Z")))

        val result = sut.checkAndUpdate()

        assertEquals(Either.Right(Unit), result)
        verify(referenceService).updateLatestPublishedAt(Instant.parse("2026-08-01T00:00:00Z"))
    }

    @Test
    fun `wraps a platform error into the required platform binary error`() {
        whenever(platformService.fetchPlatformName())
            .thenReturn(Either.Left(PlatformError.UNABLE_TO_DEFINE_PLATFORM))

        val result = sut.checkAndUpdate()

        assertEquals(Either.Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_REQUIRED_PLATFORM), result)
        verify(referenceService, never()).updateLatestPublishedAt(any())
    }

    @Test
    fun `re-fetches the binary when a newer version is available`() {
        File(tempDir, "binaries/fb2c").apply { parentFile.mkdirs() }.writeText("local binary")
        whenever(fetchService.fetchLatestPublishedAt())
            .thenReturn(Either.Right(Instant.parse("2026-08-10T00:00:00Z")))
        whenever(referenceService.fetchLatestPublishedAt()).thenReturn(Instant.parse("2026-08-01T00:00:00Z"))
        whenever(platformService.fetchPlatformName()).thenReturn(Either.Right("darwin-arm64"))
        whenever(fetchService.fetchForPlatform("darwin-arm64"))
            .thenReturn(Either.Right(Instant.parse("2026-08-10T00:00:00Z")))

        val result = sut.checkAndUpdate()

        assertEquals(Either.Right(Unit), result)
        verify(fetchService).fetchForPlatform("darwin-arm64")
        verify(referenceService).updateLatestPublishedAt(Instant.parse("2026-08-10T00:00:00Z"))
    }

    @Test
    fun `reports no newest versions when the stored date is not older`() {
        File(tempDir, "binaries/fb2c").apply { parentFile.mkdirs() }.writeText("local binary")
        whenever(fetchService.fetchLatestPublishedAt())
            .thenReturn(Either.Right(Instant.parse("2026-08-01T00:00:00Z")))
        whenever(referenceService.fetchLatestPublishedAt()).thenReturn(Instant.parse("2026-08-01T00:00:00Z"))

        val result = sut.checkAndUpdate()

        assertEquals(Either.Left(BinaryError.NO_NEWEST_VERSIONS), result)
        verify(referenceService, never()).updateLatestPublishedAt(any())
    }

    @Test
    fun `propagates a fetch error when checking the latest published date fails`() {
        File(tempDir, "binaries/fb2c").apply { parentFile.mkdirs() }.writeText("local binary")
        whenever(fetchService.fetchLatestPublishedAt())
            .thenReturn(Either.Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_CONTENT))

        val result = sut.checkAndUpdate()

        assertEquals(Either.Left(BinaryError.UNABLE_TO_FETCH_BINARY_NO_CONTENT), result)
        verify(referenceService, never()).updateLatestPublishedAt(any())
    }
}
