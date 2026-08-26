package org.grakovne.sideload.kindle.user.reference.service

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.repository.UserDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserServiceTest : TestDatabase() {

    @Autowired
    lateinit var dao: UserDao

    private lateinit var sut: UserService

    @BeforeEach
    fun setUp() {
        sut = UserService(dao)
    }

    @Test
    fun `creates a free user with the language and activity timestamp`() {
        val user = sut.fetchOrCreateUser("user-1", "ru")

        assertEquals("user-1", user.id)
        assertEquals("ru", user.language)
        assertEquals(Type.FREE_USER, user.type)
        assertTrue(user.lastActivityTimestamp != null)
    }

    @Test
    fun `reuses an existing user and keeps the stored language`() {
        dao.save(
            User(
                id = "user-1",
                language = "en",
                type = Type.SUPER_USER,
                lastActivityTimestamp = Instant.parse("2026-08-01T00:00:00Z")
            )
        )

        val user = sut.fetchOrCreateUser("user-1", "ru")

        assertEquals(Type.SUPER_USER, user.type)
        assertEquals("en", user.language)
        assertEquals(1, dao.count())
    }

    @Test
    fun `falls back to the requested language when the stored one is missing`() {
        dao.save(
            User(
                id = "user-1",
                language = null,
                type = Type.SUPER_USER,
                lastActivityTimestamp = Instant.parse("2026-08-01T00:00:00Z")
            )
        )

        val user = sut.fetchOrCreateUser("user-1", "ru")

        assertEquals(Type.SUPER_USER, user.type)
        assertEquals("ru", user.language)
    }

    @Test
    fun `fetches the user by id`() {
        dao.save(user("user-1", Type.FREE_USER))

        val fetched = sut.fetchUser("user-1")

        assertEquals("user-1", fetched.id)
    }

    @Test
    fun `fetches active users inside the activity window`() {
        dao.saveAll(
            listOf(
                userWithActivity("user-1", Instant.parse("2026-08-01T00:00:00Z")),
                userWithActivity("user-2", Instant.parse("2026-08-03T00:00:00Z")),
                userWithActivity("user-3", Instant.parse("2026-08-05T00:00:00Z"))
            )
        )

        val active = sut.fetchActiveUsers(
            Instant.parse("2026-08-02T00:00:00Z"),
            Instant.parse("2026-08-04T00:00:00Z")
        )

        assertEquals(listOf("user-2"), active.map { it.id })
    }

    @Test
    fun `fetches only the super users`() {
        dao.saveAll(
            listOf(
                user("user-1", Type.FREE_USER),
                user("user-2", Type.SUPER_USER),
                user("user-3", Type.FREE_USER)
            )
        )

        val superUsers = sut.fetchSuperUsers()

        assertEquals(listOf("user-2"), superUsers.map { it.id })
    }

    private fun user(id: String, type: Type) = userWithActivity(id, Instant.now(), type)

    private fun userWithActivity(
        id: String,
        lastActivity: Instant,
        type: Type = Type.FREE_USER
    ) = User(
        id = id,
        language = "en",
        type = type,
        lastActivityTimestamp = lastActivity
    )
}
