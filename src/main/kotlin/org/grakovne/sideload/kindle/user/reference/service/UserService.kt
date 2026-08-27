package org.grakovne.sideload.kindle.user.reference.service

import mu.KotlinLogging
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.repository.UserDao
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserService(private val userRepository: UserDao) {

    fun fetchActiveUsers(from: Instant, to: Instant) = userRepository.findByLastActivityTimestampGreaterThanAndLastActivityTimestampLessThan(from, to)

    fun fetchSuperUsers() = userRepository.findByType(Type.SUPER_USER)

    fun fetchUser(userId: String): User = userRepository
        .findById(userId)
        ?: throw NoSuchElementException("User $userId not found")

    fun fetchOrCreateUser(userId: String, language: String): User =
        userRepository
            .findById(userId)
            ?.let { persistUser(it.id, language, it.type, it.lastActivityTimestamp) }
            ?: persistUser(userId, language, Type.FREE_USER, Instant.now())

    private fun persistUser(
        id: String,
        language: String,
        type: Type,
        lastActivityTimestamp: Instant?
    ): User = User(
        id = id,
        language = language,
        type = type,
        lastActivityTimestamp = lastActivityTimestamp
    ).let { userRepository.save(it) }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
