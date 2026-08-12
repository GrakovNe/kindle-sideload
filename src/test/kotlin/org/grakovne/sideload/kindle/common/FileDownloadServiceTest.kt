package org.grakovne.sideload.kindle.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FileDownloadServiceTest {

    private val service = FileDownloadService(
        object : org.springframework.web.client.RestTemplate() {}
    )

    @Test
    fun `should transliterate Cyrillic characters to ASCII`() {
        val result = service.createSafeTempFile("тест.fb2")
        assertTrue(result.name.contains("test"), "Filename should contain transliterated 'test'")
        assertTrue(result.name.endsWith(".fb2"))
    }

    @Test
    fun `should remove special characters`() {
        val result = service.createSafeTempFile("file<name>\"test\".fb2")
        assertEquals("file_name_test.fb2", result.name)
    }

    @Test
    fun `should replace multiple underscores with single underscore`() {
        val result = service.createSafeTempFile("file___name.fb2")
        assertEquals("file_name.fb2", result.name)
    }

    @Test
    fun `should return file for empty input`() {
        val result = service.createSafeTempFile("")
        assertEquals("file", result.name)
    }

    @Test
    fun `should return file for whitespace-only input`() {
        val result = service.createSafeTempFile("   ")
        assertEquals("file", result.name)
    }

    @Test
    fun `should preserve file extension lowercase`() {
        val result = service.createSafeTempFile("document.PDF")
        assertEquals("document.pdf", result.name)
    }

    @Test
    fun `should handle input without extension`() {
        val result = service.createSafeTempFile("readme")
        assertEquals("readme", result.name)
    }

    @Test
    fun `should trim leading and trailing whitespace`() {
        val result = service.createSafeTempFile("  file.txt  ")
        assertEquals("file.txt", result.name)
    }

    @Test
    fun `should handle null input`() {
        val result = service.createSafeTempFile(null)
        assertEquals("file", result.name)
    }

    @Test
    fun `should not allow path traversal`() {
        val result = service.createSafeTempFile("../etc/passwd.txt")
        assertFalse(result.absolutePath.contains("../"))
    }

    @Test
    fun `should handle multiple dots in filename`() {
        // archive.tar.gz: base="archive.tar", ext=".gz"
        // '.' is replaced with '_' -> "archive_tar", ext preserved as ".gz"
        // Final: "archive_tar.gz"
        val result = service.createSafeTempFile("archive.tar.gz")
        assertEquals("archive_tar.gz", result.name)
    }

    @Test
    fun `should handle Cyrillic filename with special chars`() {
        val result = service.createSafeTempFile("документ_v2.0 (финал).fb2")
        assertTrue(result.name.contains("dokument"), "Should contain transliterated 'dokument'")
        assertTrue(result.name.endsWith(".fb2"), "Should preserve extension")
    }
}
