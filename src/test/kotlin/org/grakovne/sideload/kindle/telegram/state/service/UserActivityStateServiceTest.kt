package org.grakovne.sideload.kindle.telegram.state.service

import org.grakovne.sideload.kindle.telegram.state.domain.UserActivityState
import org.grakovne.sideload.kindle.telegram.state.repository.UserActivityStateRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
class UserActivityStateServiceTest {

    @Autowired
    lateinit var repository: UserActivityStateRepository

    private lateinit var sut: UserActivityStateService

    @BeforeEach
    fun setUp() {
        sut = UserActivityStateService(repository)
    }

    private fun state(userId: String, activityState: String, createdAt: Instant) =
        UserActivityState(UUID.randomUUID(), userId, activityState, createdAt)

    @Test
    fun `returns null when the user has no recorded state`() {
        assertNull(sut.fetchCurrentState("user-1"))
    }

    @Test
    fun `returns the most recently created state`() {
        repository.save(state("user-1", "OLD", Instant.now().minusSeconds(10)))
        repository.save(state("user-1", "NEW", Instant.now()))

        assertEquals("NEW", sut.fetchCurrentState("user-1"))
    }

    @Test
    fun `returns only the state of the requested user`() {
        repository.save(state("user-1", "A", Instant.now()))
        repository.save(state("user-2", "B", Instant.now().plusSeconds(1)))

        assertEquals("A", sut.fetchCurrentState("user-1"))
    }

    @Test
    fun `persists the new state so it becomes the current one`() {
        val result = sut.setCurrentState("user-1", "CONVERTING")

        assertTrue(result.isRight())
        assertEquals("CONVERTING", sut.fetchCurrentState("user-1"))
        val row = repository.findAll().single()
        assertEquals("user-1", row.userId)
        assertEquals("CONVERTING", row.activityState)
    }

    @Test
    fun `stores an empty string when the new state is null`() {
        val result = sut.setCurrentState("user-1", null)

        assertTrue(result.isRight())
        assertEquals("", repository.findAll().single().activityState)
        assertEquals("", sut.fetchCurrentState("user-1"))
    }
}
