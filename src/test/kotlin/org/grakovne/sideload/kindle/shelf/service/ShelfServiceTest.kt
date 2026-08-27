package org.grakovne.sideload.kindle.shelf.service

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.generated.tables.ShelfReference.Companion.SHELF_REFERENCE
import org.grakovne.sideload.kindle.shelf.configuration.ShelfWebProperties
import org.grakovne.sideload.kindle.shelf.domain.ShelfContentItem
import org.grakovne.sideload.kindle.shelf.domain.ShelfItem
import org.grakovne.sideload.kindle.shelf.domain.ShelfItemStatus
import org.grakovne.sideload.kindle.shelf.repository.ShelfReferenceDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShelfServiceTest : TestDatabase() {

    @Autowired
    lateinit var shelfReferenceDao: ShelfReferenceDao

    private val shelfItemService = mock<ShelfItemService>()
    private val environmentService = mock<UserEnvironmentService>()

    private lateinit var sut: ShelfService

    @BeforeEach
    fun setUp() {
        val properties = ShelfWebProperties()
        properties.hostName = "http://shelf.example.com"
        sut = ShelfService(shelfItemService, environmentService, shelfReferenceDao, properties)
    }

    @Test
    fun `creates the shelf once and reuses it on subsequent calls`() {
        sut.fetchOrCreateShelf("user-1")
        sut.fetchOrCreateShelf("user-1")

        val stored = dsl
            .select(SHELF_REFERENCE.ID, SHELF_REFERENCE.USER_ID, SHELF_REFERENCE.SHORT_ID)
            .from(SHELF_REFERENCE)
            .fetchSingle()

        assertEquals("user-1", stored.value2())
        val shortId = assertNotNull(stored.value3())
        assertTrue(shortId.matches(Regex("[a-zA-Z]{5}")))
        assertEquals(1, shelfReferenceDao.count())
    }

    @Test
    fun `creates a separate shelf per user`() {
        val first = sut.fetchOrCreateShelf("user-1")
        val second = sut.fetchOrCreateShelf("user-2")

        assertTrue(first.id != second.id)
        assertEquals(2, shelfReferenceDao.count())
    }

    @Test
    fun `resolves the user id by the short shelf id`() {
        val shelf = sut.fetchOrCreateShelf("user-1")

        assertEquals("user-1", sut.fetchUserId(shelf.shortId))
        assertNull(sut.fetchUserId("unknown"))
    }

    @Test
    fun `builds the public shelf link from the host name and the short id`() {
        val link = sut.fetchShelfLink("user-1")

        assertTrue(link.startsWith("http://shelf.example.com/"))
        assertEquals(5, link.length - "http://shelf.example.com/".length)
    }

    @Test
    fun `joins the active shelf items with their environment files`() {
        val shelf = sut.fetchOrCreateShelf("user-1")
        val env1Files = listOf(File("/env/1/book.azw3"))
        val env2Files = listOf(File("/env/2/first.epub"), File("/env/2/second.epub"))

        whenever(shelfItemService.provideShelfItems(eq(shelf.id)))
            .thenReturn(
                listOf(
                    item(shelf.id, "env-1", Instant.parse("2026-08-01T09:00:00Z")),
                    item(shelf.id, "env-2", Instant.parse("2026-08-01T10:00:00Z"))
                )
            )
        whenever(environmentService.provideEnvironmentFiles(eq("env-1"))).thenReturn(env1Files)
        whenever(environmentService.provideEnvironmentFiles(eq("env-2"))).thenReturn(env2Files)

        val content = sut.fetchShelfContent(shelf.shortId)

        assertEquals(
            listOf(
                ShelfContentItem(file = env1Files[0], createdAt = Instant.parse("2026-08-01T09:00:00Z"), environmentId = "env-1"),
                ShelfContentItem(file = env2Files[0], createdAt = Instant.parse("2026-08-01T10:00:00Z"), environmentId = "env-2"),
                ShelfContentItem(file = env2Files[1], createdAt = Instant.parse("2026-08-01T10:00:00Z"), environmentId = "env-2")
            ),
            content
        )
    }

    @Test
    fun `returns an empty content for an unknown shelf`() {
        assertEquals(emptyList<ShelfContentItem>(), sut.fetchShelfContent("unknown"))
    }

    private fun item(shelfId: UUID, environmentId: String, createdAt: Instant) = ShelfItem(
        id = UUID.randomUUID(),
        shelfId = shelfId,
        environmentId = environmentId,
        createdAt = createdAt,
        status = ShelfItemStatus.ACTIVE
    )
}
