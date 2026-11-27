package org.grakovne.sideload.kindle.shelf.web.endpoint

import org.grakovne.sideload.kindle.KindleSideloadApplication
import org.grakovne.sideload.kindle.NoOpScheduling
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Boots the real application so the landing page is rendered by the actual Thymeleaf engine:
 * a broken template or a route shadowed by the `/{shortUserId}` shelf mapping would only show up
 * against a live context.
 */
@SpringBootTest(
    classes = [KindleSideloadApplication::class, NoOpScheduling::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class LandingEndpointRenderingTest {

    @LocalServerPort
    private var port: Int = 0

    private val client: HttpClient = HttpClient.newHttpClient()

    private fun fetch(language: String? = null): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI("http://127.0.0.1:$port/"))
        language?.let { builder.header("Accept-Language", it) }
        return client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `serves the landing page instead of a 404 on the root path`() {
        val response = fetch()

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("Kindle Sideload Web"))
        assertTrue(response.body().contains("Telegram bot"))
    }

    @Test
    fun `renders the landing page in russian for a russian browser`() {
        val response = fetch("ru-RU,ru;q=0.8")

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("книжная полка"), response.body())
    }

    @Test
    fun `centers the landing notice in every served language`() {
        val served = listOf(
            null to LandingCopy("english", "book shelf lives at a personal address"),
            "ru-RU,ru;q=0.8" to LandingCopy("russian", "книжная полка живёт по личному адресу"),
        )

        served.forEach { (acceptLanguage, copy) ->
            val body = fetch(acceptLanguage).body()

            assertTrue(
                body.contains(copy.marker),
                "expected the ${copy.label} landing copy, got: $body"
            )

            val notice = body.substringAfter(".notice {").substringBefore("}")
            assertTrue(
                notice.contains("text-align: center") && notice.contains("align-items: center"),
                "the ${copy.label} landing notice is not centered: $notice"
            )
        }
    }

    private data class LandingCopy(val label: String, val marker: String)

    @Test
    fun `does not shadow the shelf route`() {
        // an unregistered shelf id still goes to the shelf handler, not to the landing page
        val response = client.send(
            HttpRequest.newBuilder(URI("http://127.0.0.1:$port/zzzzz")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("No files available"), response.body())
    }
}
