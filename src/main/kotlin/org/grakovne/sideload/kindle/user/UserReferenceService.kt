package org.grakovne.sideload.kindle.user

import org.grakovne.sideload.kindle.user.domain.Type
import org.grakovne.sideload.kindle.user.domain.UserReference
import org.grakovne.sideload.kindle.user.domain.UserReferenceSource
import org.grakovne.sideload.kindle.user.repository.UserReferenceRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UserReferenceService(private val userReferenceRepository: UserReferenceRepository) {

    fun fetchSuperUsers() = userReferenceRepository.findByType(Type.SUPER_USER)


    fun fetchUser(userId: String, source: UserReferenceSource, language: String): UserReference =
        userReferenceRepository
            .findById(userId)
            .orElseGet { persistUser(userId, source, language, Type.FREE_USER) }
            .copy(language = language)
            .let { persistUser(it.id, it.source, it.language ?: "en", it.type) }


    private fun persistUser(
        id: String,
        source: UserReferenceSource,
        language: String,
        type: Type
    ): UserReference = UserReference(
        id = id,
        source = source,
        language = language,
        type = type,
        lastActivityTimestamp = Instant.now()
    ).let { userReferenceRepository.save(it) }
}