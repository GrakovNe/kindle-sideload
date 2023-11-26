package org.grakovne.sideload.kindle.user.reference.repository

import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface UserRepository : JpaRepository<User, String> {

    fun findByLastActivityTimestampGreaterThanAndLastActivityTimestampLessThan(
        from: Instant,
        to: Instant
    ): List<User>

    fun findByType(type: Type): List<User>
}