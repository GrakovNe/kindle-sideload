package org.grakovne.sideload.kindle.shelf.web.localization

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LocalizedTemplateProviderTest {

    private val sut = LocalizedTemplateProvider()

    @Test
    fun `uses the localized template when it exists for the language`() {
        assertEquals("shelf_ru", sut.provideLocalized("shelf", "ru"))
    }

    @Test
    fun `falls back to the base template when the localization is missing`() {
        assertEquals("shelf", sut.provideLocalized("shelf", "fr"))
    }

    @Test
    fun `uses the base template when the language is unknown`() {
        assertEquals("shelf", sut.provideLocalized("shelf", null))
    }
}
