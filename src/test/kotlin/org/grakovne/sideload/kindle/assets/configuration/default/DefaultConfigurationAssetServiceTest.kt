package org.grakovne.sideload.kindle.assets.configuration.default

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultConfigurationAssetServiceTest {

    @Test
    fun `provides the default configuration asset from the classpath`() {
        val service = DefaultConfigurationAssetService()

        val file = service.fetchDefaultConfiguration()

        assertTrue(file != null && file.exists())
        assertTrue(file.name.startsWith("default_"))
        assertTrue(file.name.endsWith(".zip"))
        assertTrue(file.length() > 0L)
    }

    @Test
    fun `caches the temporary file between calls`() {
        val service = DefaultConfigurationAssetService()

        val first = service.fetchDefaultConfiguration()
        val second = service.fetchDefaultConfiguration()

        assertEquals(first, second)
    }
}
