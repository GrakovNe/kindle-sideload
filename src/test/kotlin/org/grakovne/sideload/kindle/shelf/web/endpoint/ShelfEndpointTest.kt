package org.grakovne.sideload.kindle.shelf.web.endpoint

import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.shelf.converter.ShelfContentItemConverter
import org.grakovne.sideload.kindle.shelf.domain.ShelfContentItem
import org.grakovne.sideload.kindle.shelf.service.ShelfService
import org.grakovne.sideload.kindle.shelf.web.localization.LocalizedTemplateProvider
import org.grakovne.sideload.kindle.shelf.web.view.ShelfContentItemView
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.ui.Model
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShelfEndpointTest {

    private val shelfService = mock<ShelfService>()
    private val converter = mock<ShelfContentItemConverter>()
    private val environmentService = mock<UserEnvironmentService>()
    private val userService = mock<UserService>()
    private val templateProvider = mock<LocalizedTemplateProvider>()
    private val model = mock<Model>()

    private lateinit var sut: ShelfEndpoint

    @BeforeEach
    fun setUp() {
        sut = ShelfEndpoint(
            shelfService,
            converter,
            environmentService,
            userService,
            templateProvider
        )
    }

    @Test
    fun `renders the shelf with the localized template for a russian user`() {
        val newDate = Instant.parse("2026-08-01T10:00:00Z")
        val oldDate = Instant.parse("2026-08-01T09:00:00Z")
        val newer = contentItem("/env/2/new.epub", "env-2", newDate)
        val older = contentItem("/env/1/old.epub", "env-1", oldDate)
        val views = listOf(view("new.epub", "env-2"), view("old.epub", "env-1"))

        whenever(shelfService.fetchShelfContent("abcde")).thenReturn(listOf(older, newer))
        whenever(converter.apply(older)).thenReturn(views[1])
        whenever(converter.apply(newer)).thenReturn(views[0])
        whenever(shelfService.fetchUserId("abcde")).thenReturn("user-1")
        whenever(userService.fetchUser("user-1")).thenReturn(user(language = "ru"))
        whenever(templateProvider.provideLocalized(eq("shelf"), eq("ru"))).thenReturn("shelf_ru")

        val template = sut.index("abcde", model)

        assertEquals("shelf_ru", template)
        // the files are ordered newest-first before the conversion
        verify(model).addAttribute(eq("files"), eq(views))
        verify(model).addAttribute(eq("currentDate"), any<String>())
        verify(model).addAttribute(eq("currentTime"), any<String>())
    }

    @Test
    fun `renders the base template when the shelf is not registered`() {
        whenever(shelfService.fetchShelfContent("unknown")).thenReturn(emptyList())
        whenever(shelfService.fetchUserId("unknown")).thenReturn(null)
        whenever(templateProvider.provideLocalized(eq("shelf"), eq("en"))).thenReturn("shelf")

        val template = sut.index("unknown", model)

        assertEquals("shelf", template)
        verify(model).addAttribute(eq("files"), eq(emptyList<ShelfContentItemView>()))
    }

    @Test
    fun `serves the environment binary for download with attachment headers`() {
        val file = File("/env/1/The Book (final).azw3")
        whenever(environmentService.provideEnvironmentFiles("env-1")).thenReturn(listOf(file))

        val response = sut.downloadBinary("env-1", "The_Book__final_.azw3")

        assertEquals(HttpStatus.OK, response.statusCode)
        val headers = response.headers
        assertEquals("attachment; filename=\"The_Book__final_.azw3\"", headers[HttpHeaders.CONTENT_DISPOSITION]?.single())
        assertEquals("application/octet-stream", headers[HttpHeaders.CONTENT_TYPE]?.single())
        assertTrue(response.body?.file == file)
    }

    private fun contentItem(path: String, environmentId: String, createdAt: Instant) = ShelfContentItem(
        environmentId = environmentId,
        file = File(path),
        createdAt = createdAt
    )

    private fun view(name: String, environmentId: String) = ShelfContentItemView(
        name = name,
        fileUrl = name,
        environmentId = environmentId
    )

    private fun user(language: String) = User(
        id = "user-1",
        language = language,
        type = Type.FREE_USER,
        lastActivityTimestamp = null
    )
}
