package org.grakovne.sideload.kindle.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TelegramTokenMaskerTest {

    private val token = "6586219940:AAF8repTIYK_mb9xBjsM-yDySV4PAYNnxD0"
    private val link = "https://api.telegram.org/file/bot$token/documents/file_7195.fb2%20%281%29"

    @Test
    fun `hides the token of a telegram file link keeping the rest of it readable`() {
        assertEquals(
            "https://api.telegram.org/file/bot***/documents/file_7195.fb2%20%281%29",
            maskTelegramToken(link)
        )
    }

    @Test
    fun `hides the token inside a longer message`() {
        val message = "404 Not Found on GET request for \"$link\": \"{\"ok\":false}\""

        val masked = maskTelegramToken(message)

        assertFalse(masked.contains(token))
        assertEquals("404 Not Found on GET request for \"https://api.telegram.org/file/bot***/documents/file_7195.fb2%20%281%29\": \"{\"ok\":false}\"", masked)
    }

    @Test
    fun `hides every occurrence of a link`() {
        val masked = maskTelegramToken("$link and once again $link")

        assertFalse(masked.contains(token))
        assertEquals(2, Regex("file/bot\\*\\*\\*/").findAll(masked).count())
    }

    @Test
    fun `leaves links without a token untouched`() {
        assertEquals("https://github.com/releases/fb2cng.tar.gz", maskTelegramToken("https://github.com/releases/fb2cng.tar.gz"))
        assertEquals("a file/botless sentence", maskTelegramToken("a file/botless sentence"))
    }

    @Test
    fun `masks the token of an arbitrary rendered object`() {
        val task = ConvertationTaskProbe(link)

        assertEquals("task(https://api.telegram.org/file/bot***/documents/file_7195.fb2%20%281%29)", maskTelegramToken(task))
    }

    @Test
    fun `renders a missing value the same way string interpolation does`() {
        assertEquals("null", maskTelegramToken(null))
    }

    private class ConvertationTaskProbe(private val sourceFileUrl: String) {
        override fun toString(): String = "task($sourceFileUrl)"
    }
}
