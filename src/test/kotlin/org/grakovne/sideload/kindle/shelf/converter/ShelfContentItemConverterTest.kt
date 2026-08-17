package org.grakovne.sideload.kindle.shelf.converter

import org.grakovne.sideload.kindle.shelf.domain.ShelfContentItem
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals

class ShelfContentItemConverterTest {

    private val sut = ShelfContentItemConverter()

    @Test
    fun `maps the content item to its view with the sanitized file url`() {
        val item = ShelfContentItem(
            environmentId = "env-1",
            file = File("/env/1/The Book (final).azw3"),
            createdAt = Instant.parse("2026-08-01T00:00:00Z")
        )

        val view = sut.apply(item)

        assertEquals("The Book (final).azw3", view.name)
        assertEquals("The_Book__final_.azw3", view.fileUrl)
        assertEquals("env-1", view.environmentId)
    }
}
