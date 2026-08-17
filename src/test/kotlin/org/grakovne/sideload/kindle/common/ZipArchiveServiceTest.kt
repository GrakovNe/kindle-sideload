package org.grakovne.sideload.kindle.common

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZipArchiveServiceTest {

    @TempDir
    lateinit var tempDir: File

    private val sut = ZipArchiveService()

    private fun buildZip(target: File, entries: Map<String, String>) {
        ZipOutputStream(FileOutputStream(target)).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }

    @Test
    fun `unpacks the archive into the target folder preserving the entry structure`() {
        val target = File(tempDir, "target").apply { mkdirs() }
        val zipFile = File(tempDir, "archive.zip")
        buildZip(
            zipFile,
            mapOf(
                "configuration.toml" to "key = value",
                "covers/default_cover.jpeg" to "jpeg-bytes"
            )
        )

        assertDoesNotThrow { sut.unpack(zipFile, target) }

        assertEquals("key = value", File(target, "configuration.toml").readText())
        assertEquals("jpeg-bytes", File(target, "covers/default_cover.jpeg").readText())
    }

    @Test
    fun `throws on a corrupt archive`() {
        val corrupt = File(tempDir, "corrupt.zip").apply { writeBytes("this is not a zip".toByteArray()) }
        val target = File(tempDir, "corrupt-target").apply { mkdirs() }

        assertFailsWith<net.lingala.zip4j.exception.ZipException> {
            sut.unpack(corrupt, target)
        }
    }
}
