package org.grakovne.sideload.kindle.converter.binary.unpack

import org.grakovne.sideload.kindle.converter.binary.configuration.ConverterBinaryProperties
import org.grakovne.sideload.kindle.converter.binary.provider.ConverterBinaryProvider
import org.grakovne.sideload.kindle.converter.binary.reference.domain.BinaryError
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchivedBinaryUnpackServiceTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var binaryFolder: File
    private lateinit var sut: ArchivedBinaryUnpackService

    @BeforeEach
    fun setUp() {
        binaryFolder = File(tempDir, "binaries")
        val properties = ConverterBinaryProperties().apply {
            binaryPersistencePath = binaryFolder.absolutePath
            converterFileName = "fb2c"
        }
        sut = ArchivedBinaryUnpackService(ConverterBinaryProvider(properties))
    }

    @Test
    fun `extracts the archive content into the binary folder`() {
        val archive = createZip("fb2c" to "fake converter binary")

        val result = sut.unpack(archive)

        assertTrue(result.isRight())
        assertTrue(File(binaryFolder, "fb2c").readText() == "fake converter binary")
    }

    @Test
    fun `reports an unpack error for a corrupt archive`() {
        val archive = File(tempDir, "corrupt.zip").apply { writeText("this is not a zip archive at all") }

        val result = sut.unpack(archive)

        assertEquals(BinaryError.UNABLE_TO_UNPACK_BINARY, result.swap().orNull())
    }

    @Test
    fun `reports an unpack error when the archive is missing`() {
        val result = sut.unpack(File(tempDir, "does-not-exist.zip"))

        assertEquals(BinaryError.UNABLE_TO_UNPACK_BINARY, result.swap().orNull())
    }

    private fun createZip(vararg entries: Pair<String, String>): File {
        val file = File(tempDir, "archive.zip")
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }
}
