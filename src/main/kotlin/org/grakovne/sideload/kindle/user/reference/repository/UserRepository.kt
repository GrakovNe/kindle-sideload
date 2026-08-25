package org.grakovne.sideload.kindle.user.reference.repository

import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface UserRepository : JpaRepository<User, String> {

    fun findByLastActivityTimestampGreaterThanAndLastActivityTimestampLessThan(
        from: Instant,
        to: Instant
    ): List<User>

    fun findByType(type: Type): List<User>

    @Modifying(clearAutomatically = true)
    @Query("update User u set u.lastActivityTimestamp = :timestamp where u.id = :id")
    fun touchLastActivity(@Param("id") id: String, @Param("timestamp") timestamp: Instant): Int
}