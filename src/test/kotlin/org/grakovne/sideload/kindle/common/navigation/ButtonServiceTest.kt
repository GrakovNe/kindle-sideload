package org.grakovne.sideload.kindle.common.navigation

import org.grakovne.sideload.kindle.telegram.handlers.screens.convertation.SendConvertedToEmailButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestSettingButton
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class ButtonServiceTest {

    private val buttonService = ButtonService()

    @Test
    fun `fetches an object button by its name`() {
        assertSame(RequestSettingButton, buttonService.instance("RequestSettingButton"))
    }

    @Test
    fun `fetches a class button by its qualified name with payload`() {
        // the payload part is stripped, so the qualified name resolves to the same button type
        val button = buttonService.instance("SendConvertedToEmailButton#env-1")
        assertNotNull(button)
        assertEquals("SendConvertedToEmailButton", button.javaClass.simpleName)
    }

    @Test
    fun `returns null for unknown button`() {
        assertNull(buttonService.instance("NoSuchButton"))
    }

    @Test
    fun `returns the same instance for repeated lookups`() {
        assertSame(buttonService.instance("RequestSettingButton"), buttonService.instance("RequestSettingButton"))
    }
}
