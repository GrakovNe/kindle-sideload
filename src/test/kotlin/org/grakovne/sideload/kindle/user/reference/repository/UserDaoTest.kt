package org.grakovne.sideload.kindle.user.reference.repository

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserDaoTest : TestDatabase() {

    @Autowired
    private lateinit var userRepository: UserDao

    @Test
    fun `reads back a user whose language was never stored`() {
        userRepository.save(User("language-less-user", null, Type.FREE_USER, Instant.now()))

        val fetched = userRepository.findById("language-less-user")

        assertNotNull(fetched)
        assertNull(fetched.language)
    }

    @Test
    fun `reads back a stored user`() {
        val now = Instant.now()
        userRepository.save(User("ru-user", "ru", Type.SUPER_USER, now))

        val fetched = userRepository.findById("ru-user")

        assertNotNull(fetched)
        assertEquals("ru", fetched.language)
        assertEquals(Type.SUPER_USER, fetched.type)
    }
}
