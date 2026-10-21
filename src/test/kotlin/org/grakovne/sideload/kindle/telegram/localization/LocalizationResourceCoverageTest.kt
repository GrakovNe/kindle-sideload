package org.grakovne.sideload.kindle.telegram.localization

import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import tools.jackson.core.type.TypeReference
import kotlin.test.assertTrue

class LocalizationResourceCoverageTest {

    private val productionMessageClasses: List<Class<*>> = Reflections(
        ConfigurationBuilder().forPackages("org.grakovne.sideload.kindle")
    )
        .getSubTypesOf(Message::class.java)
        .toList()
        .filterNot { it.isFromTestClasspath() }

    @Test
    fun `every production button has a template in the default buttons resource`() {
        val templateNames = fetchTemplateNames("buttons.json")
        val missing = productionMessageClasses
            .filter { it != Button::class.java && Button::class.java.isAssignableFrom(it) }
            .map { it.simpleName }
            .filterNot(templateNames::contains)

        assertTrue(
            missing.isEmpty(),
            "Buttons without a template in buttons.json: ${missing.sorted()}"
        )
    }

    @Test
    fun `every production message has a template in the default messages resource`() {
        val templateNames = fetchTemplateNames("messages.json")
        val missing = productionMessageClasses
            .filterNot { Button::class.java.isAssignableFrom(it) }
            .map { it.simpleName }
            .filterNot(templateNames::contains)

        assertTrue(
            missing.isEmpty(),
            "Messages without a template in messages.json: ${missing.sorted()}"
        )
    }

    private fun fetchTemplateNames(resource: String): Set<String> = kotlinMapper()
        .readValue(
            javaClass.classLoader.getResourceAsStream(resource)!!.readBytes(),
            object : TypeReference<List<Map<String, Any?>>>() {}
        )
        .mapNotNull { it["name"] as? String }
        .toSet()

    private fun Class<*>.isFromTestClasspath(): Boolean =
        protectionDomain?.codeSource?.location?.toString()?.contains("/build/classes/kotlin/test/") == true
}
