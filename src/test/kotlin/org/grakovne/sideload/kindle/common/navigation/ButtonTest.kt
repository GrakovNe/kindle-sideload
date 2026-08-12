package org.grakovne.sideload.kindle.common.navigation

import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ButtonTest {

    @Test
    fun `should fetch button name from qualified string without payload`() {
        assertEquals("MainMenuButton", Button.fetchButtonName("MainMenuButton"))
    }

    @Test
    fun `should fetch button name from qualified string with payload`() {
        assertEquals("ActionBtn", Button.fetchButtonName("ActionBtn#action1"))
    }

    @Test
    fun `should fetch button payload from qualified string`() {
        assertEquals("action1", Button.fetchButtonPayload("ActionBtn#action1"))
    }

    @Test
    fun `should return whole string when no payload delimiter`() {
        // When there's no #, split returns a single element, and last() gives the full string
        assertEquals("ActionBtn", Button.fetchButtonPayload("ActionBtn"))
    }

    @Test
    fun `should handle payload with hash characters`() {
        // split by '#' then last() — splits into [Button, payload, with, hashes]
        // last element is "hashes"
        assertEquals("hashes", Button.fetchButtonPayload("Button#payload#with#hashes"))
    }

    @Test
    fun `should handle payload with single hash`() {
        assertEquals("value", Button.fetchButtonPayload("Btn#value"))
    }

    @Test
    fun `should return null payload for button without payload`() {
        val button: Button = object : Button() {}
        assertEquals(null, button.payload)
    }

    @Test
    fun `equals should match by simpleName`() {
        val btn1: Button = object : Button() {}
        val btn2: Button = object : Button() {}
        assertEquals(btn1, btn2)
        assertEquals(btn1.hashCode(), btn2.hashCode())
    }
}
