package org.grakovne.sideload.kindle.assets.configuration.default

import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The converter binary is auto-updated from GitHub releases and decodes its configuration with
 * unknown fields rejected, so a key the shipped binary no longer accepts aborts every conversion
 * that falls back to the bundled default configuration. fb2cng v2.0.1 moved the Markdown image
 * setting out of `document.images`, which is exactly what broke production.
 */
class DefaultConfigurationCompatibilityTest {

    private val assetEntries: List<String>
        get() = ZipInputStream(ClassPathResource(ASSET).inputStream).use { zip ->
            generateSequence { zip.nextEntry }.map { entry -> entry.name }.toList()
        }

    private val configuration: Map<*, *> by lazy {
        val yaml = ZipInputStream(ClassPathResource(ASSET).inputStream).use { zip ->
            val entry = generateSequence { zip.nextEntry }
                .firstOrNull { it.name == CONFIGURATION_ENTRY }
            assertNotNull(entry, "the default configuration asset must contain $CONFIGURATION_ENTRY")
            zip.readAllBytes().decodeToString()
        }

        val options = LoaderOptions().apply { isAllowDuplicateKeys = false }
        @Suppress("UNCHECKED_CAST")
        Yaml(SafeConstructor(options)).load(yaml) as Map<*, *>
    }

    private fun schemaKeys(): Set<String> = ClassPathResource(SCHEMA_RESOURCE).inputStream
        .bufferedReader()
        .lineSequence()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .toSet()

    private fun paths(node: Any?, prefix: String = ""): Set<String> = when (node) {
        is Map<*, *> -> node.entries.flatMapTo(mutableSetOf()) { (key, value) ->
            val path = if (prefix.isEmpty()) key.toString() else "$prefix.$key"
            paths(value, path) + path
        }

        else -> emptySet()
    }

    private fun at(vararg path: String): Any? = path.fold(configuration as Any?) { node, key ->
        (node as? Map<*, *>)?.get(key)
    }

    @Test
    fun `every key of the default configuration is known to the converter binary`() {
        val unknown = paths(configuration) - schemaKeys()

        assertTrue(
            unknown.isEmpty(),
            "the converter binary rejects unknown configuration keys, these are not in the " +
                "$CONVERTER_VERSION schema: ${unknown.sorted()}"
        )
    }

    @Test
    fun `markdown image mode lives under document markdown and not under images`() {
        val images = at("document", "images") as Map<*, *>

        assertFalse(
            images.containsKey("markdown"),
            "fb2cng v2.0.1 removed document.images.markdown, it must be document.markdown.images"
        )
        assertEquals("placeholder", at("document", "markdown", "images"))
    }

    @Test
    fun `the default configuration keeps the tuned converter behavior`() {
        assertEquals("old_kindle", at("document", "toc_type"))
        assertEquals("float", at("document", "footnotes", "mode"))
        assertEquals("keepAR", at("document", "images", "cover", "resize"))
        assertEquals(true, at("document", "text_transformations", "speech", "enable"))
        assertEquals(true, at("document", "text_transformations", "dashes", "enable"))
    }

    @Test
    fun `the default configuration ships the cover asset next to the configuration`() {
        assertEquals(listOf(CONFIGURATION_ENTRY, COVER_ENTRY), assetEntries.sorted())
    }

    companion object {
        private const val CONVERTER_VERSION = "v2.1.1"
        private const val ASSET = "assets/default_configuration.zip"
        private const val CONFIGURATION_ENTRY = "configuration.yaml"
        private const val COVER_ENTRY = "default_cover.jpeg"
        private const val SCHEMA_RESOURCE = "fb2cng/$CONVERTER_VERSION/configuration-schema-keys.txt"
    }
}
