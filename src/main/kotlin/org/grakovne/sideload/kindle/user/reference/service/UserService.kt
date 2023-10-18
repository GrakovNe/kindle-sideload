package org.grakovne.sideload.kindle.user.reference.service

import mu.KotlinLogging
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

@Service
class UserService(private val userRepository: UserRepository) {

    fun fetchSuperUsers() = userRepository.findByType(Type.SUPER_USER)

    fun fetchUser(userId: String): User? = userRepository
        .findById(userId)
        .getOrNull()

    fun fetchOrCreateUser(userId: String, language: String): User =
        userRepository
            .findById(userId)
            .orElseGet { persistUser(userId, language, Type.FREE_USER) }
            .copy(language = language)
            .let { persistUser(it.id, it.language ?: "en", it.type) }

    private fun persistUser(
        id: String,
        language: String,
        type: Type
    ): User = User(
        id = id,
        language = language,
        type = type,
        lastActivityTimestamp = Instant.now()
    ).let { userRepository.save(it) }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}