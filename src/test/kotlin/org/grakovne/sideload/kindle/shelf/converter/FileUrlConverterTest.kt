package org.grakovne.sideload.kindle.shelf.converter

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FileUrlConverterTest {

    @Test
    fun `transliterates non-latin characters and replaces unsafe characters with underscores`() {
        assertEquals("kniga_azw3", "книга azw3".toFileName())
        assertEquals("kniga__1._azw3", "книга #1. azw3".toFileName())
    }

    @Test
    fun `keeps a file name made of allowed characters intact`() {
        assertEquals("Book2024.epub", "Book2024.epub".toFileName())
    }

    @Test
    fun `replaces spaces and brackets in the file name`() {
        assertEquals("The_Book__final_.azw3", "The Book (final).azw3".toFileName())
    }
}
