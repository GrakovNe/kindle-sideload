package org.grakovne.sideload.kindle.converter.binary.provider

import org.grakovne.sideload.kindle.converter.binary.configuration.ConverterBinaryProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConverterBinaryProviderTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var properties: ConverterBinaryProperties
    private lateinit var sut: ConverterBinaryProvider

    @BeforeEach
    fun setUp() {
        properties = ConverterBinaryProperties().apply {
            binaryPersistencePath = File(tempDir, "binaries").absolutePath
            converterFileName = "fb2c"
        }
        sut = ConverterBinaryProvider(properties)
    }

    @Test
    fun `provides the binary folder and creates it if absent`() {
        val folder = sut.provideBinaryFolder()

        assertEquals(File(tempDir, "binaries"), folder)
        assertTrue(folder.isDirectory)
    }

    @Test
    fun `resolves the converter file against the binary folder`() {
        val converter = sut.provideBinaryConverter()

        assertEquals(File(tempDir, "binaries/fb2c"), converter)
    }
}
