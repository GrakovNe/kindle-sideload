package org.grakovne.sideload.kindle.telegram.localization

import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import tools.jackson.core.type.TypeReference
import java.io.File
import kotlin.reflect.full.memberProperties
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

    @Test
    fun `every placeholder in a localized template resolves to a message property`() {
        val classesByName = productionMessageClasses.associateBy { it.simpleName }
        val failures = mutableListOf<String>()

        localizedResources().forEach { (file, entries) ->
            entries.forEach { entry ->
                val name = entry["name"] as? String ?: return@forEach
                val template = entry["template"] as? String ?: return@forEach
                val target = classesByName[name] ?: return@forEach

                val properties = target.kotlin.memberProperties.map { it.name }.toSet()
                val placeholders = PLACEHOLDER_PATTERN.findAll(template).map { it.groupValues[1] }.toSet()

                (placeholders - properties)
                    .takeIf { it.isNotEmpty() }
                    ?.let { failures += "${file.name}/$name: no such property ${it.sorted()}" }

                if ("bookShelfUrl" in properties && "bookShelfUrl" !in placeholders) {
                    failures += "${file.name}/$name: declares bookShelfUrl but the template never renders it"
                }
            }
        }

        assertTrue(
            failures.isEmpty(),
            "Unrenderable placeholders:\n${failures.joinToString("\n")}"
        )
    }

    private fun localizedResources(): List<Pair<File, List<Map<String, Any?>>>> = LOCALE_FILES
        .map { File("locale", it) }
        .filter { it.exists() }
        .map { file ->
            file to kotlinMapper().readValue(
                file.readBytes(),
                object : TypeReference<List<Map<String, Any?>>>() {}
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

    companion object {
        private val PLACEHOLDER_PATTERN = Regex("""\$\{(\w+)}""")

        // LocalizationService reads these from disk relative to the working directory, so the
        // classpath resources alone are not what users actually see
        private val LOCALE_FILES = listOf(
            "messages.json",
            "messages_en.json",
            "messages_ru.json",
            "buttons.json",
            "buttons_ru.json",
        )
    }
}
