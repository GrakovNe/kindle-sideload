package org.grakovne.sideload.kindle.common

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.kotlin.any
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.ResponseExtractor
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.io.File
import java.net.ServerSocket
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileDownloadServiceTest {

    private val restTemplate: RestTemplate = mock()
    private val sut = FileDownloadService(restTemplate)

    private fun mockHttpBody(content: ByteArray): ClientHttpResponse {
        val response: ClientHttpResponse = mock()
        whenever(response.body).thenReturn(ByteArrayInputStream(content))
        return response
    }

    /**
     * Stubs [RestTemplate.execute] to route [body] through the real extractor supplied by
     * [FileDownloadService.download], so the production code path is exercised. The `RequestCallback`
     * is a literal `null` in production, so it is matched with `isNull()`; the trailing vararg is
     * intentionally left unmatched, which makes Mockito accept the (empty) vararg.
     */
    private fun bindNextDownload(body: ClientHttpResponse) {
        whenever(
            restTemplate.execute(
                any<URI>(),
                any<HttpMethod>(),
                isNull(),
                any<ResponseExtractor<*>>()
            )
        ).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val extractor = invocation.getArgument(3, ResponseExtractor::class.java)
            @Suppress("UNCHECKED_CAST")
            (extractor as ResponseExtractor<ClientHttpResponse>).extractData(body)
        }
    }

    @Test
    fun `download writes the body content into the requested file`() = runTest {
        bindNextDownload(mockHttpBody("fake epub content".toByteArray()))

        val result = sut.download("https://example.com/books/My%20Book.EPUB", "My Book.EPUB")

        assertNotNull(result)
        assertEquals("my_book.epub", result.name)
        assertEquals("fake epub content", result.readText())
        assertTrue(result.delete())
    }

    @Test
    fun `download without a file name creates a random temporary file with the source suffix`() = runTest {
        bindNextDownload(mockHttpBody("downloaded".toByteArray()))

        val result = sut.download("https://example.com/books/Secret.DOC")

        assertNotNull(result)
        assertTrue(result.name.endsWith("_Secret.DOC"))
        assertTrue(result.absolutePath.startsWith(File(System.getProperty("java.io.tmpdir")).path))
        assertEquals("downloaded", result.readText())
        assertTrue(result.delete())
    }

    @Test
    fun `download retries transient failures before succeeding`() = runTest {
        var attempts = 0
        whenever(
            restTemplate.execute(
                any<URI>(),
                any<HttpMethod>(),
                isNull(),
                any<ResponseExtractor<*>>()
            )
        ).thenAnswer { invocation ->
            attempts++
            if (attempts < 2) throw ResourceAccessException("network is down")
            @Suppress("UNCHECKED_CAST")
            val extractor = invocation.getArgument(3, ResponseExtractor::class.java)
            @Suppress("UNCHECKED_CAST")
            (extractor as ResponseExtractor<ClientHttpResponse>)
                .extractData(mockHttpBody("recovered".toByteArray()))
        }

        val result = sut.download("https://example.com/a.txt", "a.txt")

        assertEquals(2, attempts)
        assertNotNull(result)
        assertEquals("recovered", result.readText())
        assertTrue(result.delete())
    }

    @Test
    fun `download gives up after exhausting all retries`() = runTest {
        var attempts = 0
        whenever(
            restTemplate.execute(
                any<URI>(),
                any<HttpMethod>(),
                isNull(),
                any<ResponseExtractor<*>>()
            )
        ).thenAnswer {
            attempts++
            throw ResourceAccessException("always down")
        }

        val result = sut.download("https://example.com/a.txt", "a.txt")

        assertNull(result)
        assertEquals(3, attempts)
    }

    @Test
    @Timeout(20)
    fun `download requests an already encoded link without encoding it once more`() = runTest {
        // Telegram hands over an already percent encoded file link ("file_7195.fb2%20%281%29").
        // Passing it to RestTemplate as a String makes it expand the value as a uri template and
        // encode it a second time, so the request goes for a path containing a literal "%2520"
        // and Telegram answers 404. The link has to reach the wire exactly as it was given.
        val server = ServerSocket(0)
        val requestLine = CompletableFuture<String>()
        Thread {
            runCatching {
                server.accept().use { socket ->
                    requestLine.complete(socket.getInputStream().bufferedReader().readLine().orEmpty())

                    val body = "ok".toByteArray()
                    socket.getOutputStream().use { output ->
                        output.write("HTTP/1.1 200 OK\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        output.write(body)
                        output.flush()
                    }
                }
            }
        }.apply { isDaemon = true }.start()

        try {
            val link = "http://127.0.0.1:${server.localPort}/file/botTOKEN/documents/file_7195.fb2%20%281%29"

            val result = FileDownloadService(RestTemplateBuilder().build()).download(link)

            assertNotNull(result)
            assertEquals("GET /file/botTOKEN/documents/file_7195.fb2%20%281%29 HTTP/1.1", requestLine.get(10, TimeUnit.SECONDS))
            assertTrue(result.delete())
        } finally {
            runCatching { server.close() }
        }
    }

    @Nested
    inner class CreateSafeTempFileTest {

        private val tmpDir = System.getProperty("java.io.tmpdir")

        @Test
        fun `preserves the latin base and lowercases the extension`() {
            assertFileName("my_file.epub", "my-file.EPUB")
        }

        @Test
        fun `transliterates cyrillic into latin`() {
            assertFileName("moj_fajl.epub", "Мой файл.EPUB")
        }

        @Test
        fun `leaves a trailing dot when the extension has no latin letters`() {
            assertFileName("kniga.", "книга.ДОК")
        }

        @Test
        fun `replaces unsafe characters with a single separator`() {
            assertFileName("bad_name_with_spaces_txt", "bad/name with spaces!!txt")
            assertFileName("my_book_v2_name.txt", "my book (v2)!!name.TXT")
        }

        @Test
        fun `strips unsafe characters from the extension`() {
            assertFileName("file_name.zip", "file..name.ZIP!")
        }

        @Test
        fun `treats every dot in the base as a separator keeping only the last one`() {
            assertFileName("archive_tar.gz", "archive.tar.gz")
        }

        @Test
        fun `uses a default name for null, blank and dot-less bases`() {
            assertFileName("file", null)
            assertFileName("file", "   ")
            assertFileName("report", "report")
        }

        @Test
        fun `returns a file inside the system temporary folder`() {
            val file = sut.createSafeTempFile("book.epub")
            assertEquals(File(tmpDir), file.parentFile)
        }

        @Test
        fun `creation is side-effect free and points at a not-yet-existing file`() {
            // a random base keeps the assertion free of clashes with unrelated system temp files
            val randomBase = "side_effect_free_" + (System.nanoTime() % 1_000_000)
            val file = sut.createSafeTempFile("$randomBase.epub")

            assertEquals(File(tmpDir, "$randomBase.epub"), file)
            assertFalse(file.exists())
        }

        private fun assertFileName(expectedName: String, originalName: String?) {
            assertEquals(File(tmpDir, expectedName), sut.createSafeTempFile(originalName))
        }
    }
}
