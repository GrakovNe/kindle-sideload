package org.grakovne.sideload.kindle.telegram.navigation

import org.grakovne.sideload.kindle.telegram.localization.domain.Button
import org.reflections.Reflections
import org.springframework.stereotype.Service
import kotlin.reflect.full.companionObjectInstance

@Service
class ButtonService {

    private val buttons = Reflections("org.grakovne")
        .getSubTypesOf(Button::class.java)
        .associate { it.simpleName to getObjectByClassName(it.canonicalName) }

    fun fetchButtonName(button: Button): String = button.javaClass.simpleName
    fun fetchButtonForName(name: String) = buttons[name] as Button

    private fun getObjectByClassName(className: String): Any? {
        return try {
            val kClass = Class.forName(className).kotlin
            kClass.objectInstance ?: kClass.companionObjectInstance
        } catch (e: ClassNotFoundException) {
            null
        }
    }
}