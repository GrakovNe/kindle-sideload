package org.grakovne.sideload.kindle.metrics.api

import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.Instant

@DataJpaTest
class DiagTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `diag`() {
        userRepository.save(User("diag-1", "en", Type.FREE_USER, Instant.parse("2026-01-01T00:00:00Z")))
        val rows = userRepository.touchLastActivity("diag-1", Instant.parse("2026-08-25T06:00:00Z"))
        val byFind = userRepository.findUserById("diag-1")
        val byFindById = userRepository.findById("diag-1").orElse(null)
        println("DIAG rows=$rows")
        println("DIAG findUserById=${byFind?.lastActivityTimestamp}")
        println("DIAG findById=${byFindById?.lastActivityTimestamp}")
    }
}
