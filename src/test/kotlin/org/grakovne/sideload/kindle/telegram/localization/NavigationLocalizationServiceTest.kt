package org.grakovne.sideload.kindle.telegram.localization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestProjectInfoButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestSettingButton
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationLocalizationServiceTest {

    private val sut = NavigationLocalizationService(ObjectMapper().registerKotlinModule(), EnumLocalizationService(ObjectMapper().registerKotlinModule()))

    @Test
    fun `localizes the button from the english resource`() {
        val result = sut.localize(RequestSettingButton, "en")

        assertTrue(result.isRight())
        assertEquals("Settings", result.orNull()!!.text)
        assertEquals("RequestSettingButton", result.orNull()!!.action)
    }

    @Test
    fun `localizes the button from the russian resource`() {
        val result = sut.localize(RequestProjectInfoButton, "ru")

        assertTrue(result.isRight())
        assertEquals("О проекте", result.orNull()!!.text)
        assertEquals("RequestProjectInfoButton", result.orNull()!!.action)
    }

    @Test
    fun `falls back to the english resource when the language resource is missing`() {
        val result = sut.localize(RequestSettingButton, "fr")

        assertTrue(result.isRight())
        assertEquals("Settings", result.orNull()!!.text)
    }

    @Test
    fun `uses the base english resource when the language is null`() {
        val result = sut.localize(RequestSettingButton, null)

        assertTrue(result.isRight())
        assertEquals("Settings", result.orNull()!!.text)
    }

    @Test
    fun `reports the template error when there is no template for the button`() {
        val result = sut.localize(UnlocalizedTestButton(), "en")

        assertTrue(result.isLeft())
        assertEquals(LocalizationError.TEMPLATE_NOT_FOUND, result.swap().orNull())
    }

    private class UnlocalizedTestButton : Button()
}
