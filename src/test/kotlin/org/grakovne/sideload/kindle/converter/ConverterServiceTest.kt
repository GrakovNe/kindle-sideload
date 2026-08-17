package org.grakovne.sideload.kindle.converter

import arrow.core.Either
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConverterServiceTest {

    @TempDir
    lateinit var tempDir: File

    private val fb2Converter: Fb2ConverterService = mock()
    private val epubBypass: EpubBypassConverterService = mock()
    private val sut = ConverterService(fb2Converter, epubBypass)

    @Test
    fun `routes epub files to the bypass converter`() {
        val book = File(tempDir, "book.epub").apply { writeText("epub") }
        val expected = ConversionResult("bypass log", "env-1", emptyList())
        whenever(epubBypass.convertAndCollect("user-1", book)).thenReturn(Either.Right(expected))

        val result = sut.convertAndCollect("user-1", book)

        assertSame(expected, result.orNull())
        verifyNoInteractions(fb2Converter)
    }

    @Test
    fun `routing by the epub extension is case insensitive`() {
        val book = File(tempDir, "book.EPUB").apply { writeText("epub") }
        val expected = ConversionResult("bypass log", "env-1", emptyList())
        whenever(epubBypass.convertAndCollect("user-1", book)).thenReturn(Either.Right(expected))

        val result = sut.convertAndCollect("user-1", book)

        assertSame(expected, result.orNull())
        verifyNoInteractions(fb2Converter)
    }

    @Test
    fun `routes all other files to the fb2 converter`() {
        val book = File(tempDir, "book.fb2").apply { writeText("fb2") }
        val expected = ConversionResult("fb2 log", "env-1", listOf(book))
        whenever(fb2Converter.convertAndCollect("user-1", book)).thenReturn(Either.Right(expected))

        val result = sut.convertAndCollect("user-1", book)

        assertSame(expected, result.orNull())
        verifyNoInteractions(epubBypass)
    }

    @Test
    fun `propagates the converter failure`() {
        val book = File(tempDir, "book.fb2")
        whenever(fb2Converter.convertAndCollect("user-1", book))
            .thenReturn(Either.Left(UnableConvertFile("it exploded", "env-1")))

        val result = sut.convertAndCollect("user-1", book)

        assertTrue(result.isLeft())
        assertEquals("env-1", result.fold(ifLeft = { it.environmentId }, ifRight = { throw AssertionError() }))
    }

    @Test
    fun `propagates the bypass failure`() {
        val book = File(tempDir, "book.epub")
        whenever(epubBypass.convertAndCollect("user-1", book))
            .thenReturn(Either.Left(UnableDeployEnvironment))

        val result = sut.convertAndCollect("user-1", book)

        assertTrue(result.isLeft())
        assertEquals(UnableDeployEnvironment, result.fold(ifLeft = { it }, ifRight = { throw AssertionError() }))
    }
}
