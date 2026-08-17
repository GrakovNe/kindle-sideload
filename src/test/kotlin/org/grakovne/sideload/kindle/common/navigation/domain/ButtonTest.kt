package org.grakovne.sideload.kindle.common.navigation.domain

import org.grakovne.sideload.kindle.common.navigation.domain.Button.Companion.buildQualifiedName
import org.grakovne.sideload.kindle.telegram.handlers.screens.convertation.SendConvertedToEmailButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestSettingButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.MainScreenButton
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ButtonTest {

    @Test
    fun `name is the simple class name`() {
        assertEquals("RequestSettingButton", RequestSettingButton.name)
        assertEquals("SendConvertedToEmailButton", SendConvertedToEmailButton(environmentId = "env-1").name)
    }

    @Test
    fun `buildQualifiedName returns the plain name when the payload is absent`() {
        assertEquals("RequestSettingButton", RequestSettingButton.buildQualifiedName())
    }

    @Test
    fun `buildQualifiedName joins name and payload with the delimiter`() {
        assertEquals(
            "SendConvertedToEmailButton#env-1",
            SendConvertedToEmailButton(environmentId = "env-1").buildQualifiedName()
        )
    }

    @Test
    fun `fetchButtonName takes the part before the delimiter`() {
        assertEquals("SendConvertedToEmailButton", Button.fetchButtonName("SendConvertedToEmailButton#env-1"))
        assertEquals("RequestSettingButton", Button.fetchButtonName("RequestSettingButton"))
    }

    @Test
    fun `fetchButtonPayload takes the part after the delimiter`() {
        assertEquals("env-1", Button.fetchButtonPayload("SendConvertedToEmailButton#env-1"))
        assertEquals("RequestSettingButton", Button.fetchButtonPayload("RequestSettingButton"))
    }

    @Test
    fun `buttons of the same class with the same payload are equal`() {
        val first: Any = SendConvertedToEmailButton(environmentId = "env-1")
        val second: Any = SendConvertedToEmailButton(environmentId = "env-1")

        assertTrue(first == second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `buttons with different payloads are not equal`() {
        val first: Any = SendConvertedToEmailButton(environmentId = "a")
        val second: Any = SendConvertedToEmailButton(environmentId = "b")

        assertFalse(first == second)
    }

    @Test
    fun `buttons of different classes are never equal`() {
        val button: Any = SendConvertedToEmailButton(environmentId = "x")

        assertFalse(button == MainScreenButton)
        assertFalse(button == null)
    }
}
