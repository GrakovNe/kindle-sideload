package org.grakovne.sideload.kindle.shelf.web.endpoint

import org.grakovne.sideload.kindle.shelf.web.localization.LocalizedTemplateProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.ui.Model
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LandingEndpointTest {

    private val templateProvider = mock<LocalizedTemplateProvider>()
    private val model = mock<Model>()

    private lateinit var sut: LandingEndpoint

    @BeforeEach
    fun setUp() {
        sut = LandingEndpoint(templateProvider)
    }

    @Test
    fun `renders the russian landing page for a russian locale`() {
        whenever(templateProvider.provideLocalized(eq("root"), eq("ru"))).thenReturn("root_ru")

        val template = sut.landing(Locale("ru"), model)

        assertEquals("root_ru", template)
        verify(templateProvider).provideLocalized(eq("root"), eq("ru"))
    }

    @Test
    fun `falls back to the base landing page for an unsupported locale`() {
        whenever(templateProvider.provideLocalized(eq("root"), eq("de"))).thenReturn("root")

        val template = sut.landing(Locale("de"), model)

        assertEquals("root", template)
    }

    @Test
    fun `provides the clock attributes the landing page renders`() {
        whenever(templateProvider.provideLocalized(eq("root"), eq("en"))).thenReturn("root")

        sut.landing(Locale.ENGLISH, model)

        val names = argumentCaptor<String>()
        val values = argumentCaptor<Any>()
        verify(model, times(2)).addAttribute(names.capture(), values.capture())

        assertEquals(listOf("currentDate", "currentTime"), names.allValues)
        assertTrue((values.firstValue as String).matches(Regex("""\d{2}\.\d{2}\.\d{4}""")))
        assertTrue((values.secondValue as String).matches(Regex("""\d{2}:\d{2}""")))
    }
}
