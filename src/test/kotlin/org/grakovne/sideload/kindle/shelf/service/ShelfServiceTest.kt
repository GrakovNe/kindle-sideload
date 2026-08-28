package org.grakovne.sideload.kindle.shelf.service

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.shelf.configuration.ShelfWebProperties
import org.grakovne.sideload.kindle.shelf.domain.ShelfContentItem
import org.grakovne.sideload.kindle.shelf.domain.ShelfItem
import org.grakovne.sideload.kindle.shelf.domain.ShelfItemStatus
import org.grakovne.sideload.kindle.shelf.domain.ShelfReference
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShelfServiceTest : TestDatabase() {

    @Autowired
    lateinit var shelfReferenceDao: ShelfReferenceDao

    private val shelfItemService = mock<ShelfItemService>()
    private val environmentService = mock<UserEnvironmentService>()

    private lateinit var properties: ShelfWebProperties
    private lateinit var sut: ShelfService

    @BeforeEach
    fun setUp() {
        properties = ShelfWebProperties()
        properties.hostName = "http://shelf.example.com"
        sut = ShelfService(shelfItemService, environmentService, shelfReferenceDao, properties)
    }

    @Test
    fun `creates the shelf once and reuses it on subsequent calls`() {
        val first = sut.fetchOrCreateShelf("user-1")
        val second = sut.fetchOrCreateShelf("user-1")

        assertEquals(first.id, second.id)
        assertEquals("user-1", first.userId)
        assertTrue(first.shortId.matches(Regex("[a-zA-Z]{5}")))
        assertEquals(1, shelfReferenceDao.count())
    }

    @Test
    fun `retries with a new short id when the generated one is already taken`() {
        // the short id space is 26^5, so a real collision is not reproducible; reject the first
        // insert the way the unique constraint on short_id would and assert the retry recovers
        val rejectingFirstInsert = object : ShelfReferenceDao(dsl) {
            var attempts = 0

            override fun saveIfAbsent(reference: ShelfReference): ShelfReference? {
                attempts++
                return if (attempts == 1) null else super.saveIfAbsent(reference)
            }
        }
        val sut = ShelfService(shelfItemService, environmentService, rejectingFirstInsert, properties)

        val shelf = sut.fetchOrCreateShelf("user-1")

        assertEquals(2, rejectingFirstInsert.attempts)
        assertEquals("user-1", shelf.userId)
        assertTrue(shelf.shortId.matches(Regex("[a-zA-Z]{5}")))
        assertEquals(shelf, shelfReferenceDao.findByUserId("user-1"))
        assertEquals(1, shelfReferenceDao.count())
    }

    @Test
    fun `gives up instead of looping forever when no short id can be claimed`() {
        val alwaysRejecting = object : ShelfReferenceDao(dsl) {
            override fun saveIfAbsent(reference: ShelfReference): ShelfReference? = null
        }
        val sut = ShelfService(shelfItemService, environmentService, alwaysRejecting, properties)

        assertFailsWith<IllegalStateException> { sut.fetchOrCreateShelf("user-1") }
    }

    @Test
    fun `keeps the shelf a concurrent caller created first instead of overwriting it`() {
        val existing = sut.fetchOrCreateShelf("user-1")

        // the same user, a fresh id and short id — as if two callers raced past findByUserId
        val loser = shelfReferenceDao.saveIfAbsent(
            ShelfReference(id = UUID.randomUUID(), shortId = "zzzzz", userId = "user-1")
        )

        assertEquals(existing, loser)
        assertEquals(1, shelfReferenceDao.count())
        assertNull(shelfReferenceDao.findByShortId("zzzzz"))
    }

    @Test
    fun `creates a separate shelf with a distinct short id per user`() {
        val taken = sut.fetchOrCreateShelf("user-taken")

        val first = sut.fetchOrCreateShelf("user-1")
        val second = sut.fetchOrCreateShelf("user-2")

        assertTrue(first.id != second.id)
        assertEquals(3, setOf(taken.shortId, first.shortId, second.shortId).size)
        assertEquals(3, shelfReferenceDao.count())
    }

    @Test
    fun `resolves the user id by the short shelf id`() {
        val shelf = sut.fetchOrCreateShelf("user-1")

        assertEquals("user-1", sut.fetchUserId(shelf.shortId))
        assertNull(sut.fetchUserId("unknown"))
    }

    @Test
    fun `builds the public shelf link from the host name and the short id`() {
        val shelf = sut.fetchOrCreateShelf("user-1")

        assertEquals("http://shelf.example.com/${shelf.shortId}", sut.fetchShelfLink("user-1"))
        assertEquals(1, shelfReferenceDao.count())
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
