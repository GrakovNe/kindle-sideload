package org.grakovne.sideload.kindle.common.navigation

import org.grakovne.sideload.kindle.common.navigation.domain.Button
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

    fun instance(name: String): Button? {
        val buttonName = Button.fetchButtonName(name)
        return buttons[buttonName]?.let { it as Button }
    }

    private fun getObjectByClassName(className: String): Any? {
        return try {
            val kClass = Class.forName(className).kotlin
            kClass.objectInstance ?: kClass.companionObjectInstance ?: kClass.createInstance()
        } catch (e: ClassNotFoundException) {
            null
        }
    }
}