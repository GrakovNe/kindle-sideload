package org.grakovne.sideload.kindle.shelf.service

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.shelf.common.ShelfItemError
import org.grakovne.sideload.kindle.shelf.domain.ShelfItem
import org.grakovne.sideload.kindle.shelf.domain.ShelfItemStatus
import org.grakovne.sideload.kindle.shelf.repository.ShelfItemDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShelfItemServiceTest : TestDatabase() {

    @Autowired
    lateinit var dao: ShelfItemDao

    private lateinit var sut: ShelfItemService

    @BeforeEach
    fun setUp() {
        sut = ShelfItemService(dao)
    }

    @Test
    fun `attaches a new environment to the shelf`() {
        val result = sut.attachToShelf(shelfId = UUID.randomUUID(), environmentId = "env-1")

        assertTrue(result.isRight())
        val stored = dao.findByEnvironmentId("env-1")
        assertTrue(stored != null && stored.status == ShelfItemStatus.ACTIVE)
    }

    @Test
    fun `rejects attaching an environment that is already on the shelf`() {
        dao.save(item(UUID.randomUUID(), "env-1"))

        val result = sut.attachToShelf(shelfId = UUID.randomUUID(), environmentId = "env-1")

        assertTrue(result.isLeft())
        assertEquals(ShelfItemError.ITEM_ALREADY_EXISTS, result.swap().orNull())
        assertEquals(1, dao.count())
    }

    @Test
    fun `terminates an attached item`() {
        dao.save(item(UUID.randomUUID(), "env-1"))

        val result = sut.terminateItem("env-1")

        assertTrue(result.isRight())
        assertTrue(dao.findByEnvironmentId("env-1")!!.status == ShelfItemStatus.TERMINATED)
    }

    @Test
    fun `reports the missing item error when there is nothing to terminate`() {
        val result = sut.terminateItem("unknown-env")

        assertTrue(result.isLeft())
        assertEquals(ShelfItemError.ITEM_NOT_EXISTS, result.swap().orNull())
    }

    @Test
    fun `provides only the active items of the shelf`() {
        val shelfId = UUID.randomUUID()
        dao.save(item(shelfId, "env-1"))
        dao.save(item(shelfId, "env-2").copy(status = ShelfItemStatus.TERMINATED))
        dao.save(item(UUID.randomUUID(), "env-3"))

        val active = sut.provideShelfItems(shelfId)

        assertEquals(listOf("env-1"), active.map { it.environmentId })
    }

    private fun item(shelfId: UUID, environmentId: String) = ShelfItem(
        id = UUID.randomUUID(),
        shelfId = shelfId,
        environmentId = environmentId,
        createdAt = Instant.now(),
        status = ShelfItemStatus.ACTIVE
    )
}
