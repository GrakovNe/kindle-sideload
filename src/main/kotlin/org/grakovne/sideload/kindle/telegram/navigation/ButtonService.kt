package org.grakovne.sideload.kindle.telegram.navigation

import org.grakovne.sideload.kindle.telegram.localization.domain.Button
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import org.springframework.stereotype.Service
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createInstance


@Service
class ButtonService {
    private val buttons = Reflections(ConfigurationBuilder().forPackages("org.grakovne"))
        .getSubTypesOf(Button::class.java)
        .associate { it.simpleName to getObjectByClassName(it.canonicalName) }

    fun fetchButtonName(button: Button): String = button.javaClass.simpleName

    fun buildPayload(button: Button) =
        if (button.payload == null) fetchButtonName(button) else fetchButtonName(button) + "#" + button.payload

    fun fetchButtonForName(name: String): Button? {
        val buttonName = name.split(buttonPayloadDelimiter).first()
        return buttons[buttonName]?.let { it as Button }
    }

    fun fetchButtonPayload(update: String): String? {
        return update.split(buttonPayloadDelimiter).last()
    }

    private fun getObjectByClassName(className: String): Any? {
        return try {
            val kClass = Class.forName(className).kotlin
            kClass.objectInstance ?: kClass.companionObjectInstance ?: kClass.createInstance()
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    private val buttonPayloadDelimiter = "#"
}