package org.grakovne.sideload.kindle.telegram.state.service

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.telegram.state.domain.UserActivityState
import org.grakovne.sideload.kindle.telegram.state.repository.UserActivityStateRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class UserActivityStateService(
    private val repository: UserActivityStateRepository
) {

    fun fetchCurrentState(userId: String): String? = repository
        .also { logger.debug { "Fetching current activity state for $userId" } }
        .findByUserIdOrderByCreatedAtDesc(userId)
        .firstOrNull()
        ?.activityState

    fun setCurrentState(userId: String, state: String?): Either<UserActivityStateError, Unit> {
        val entity = UserActivityState(
            id = UUID.randomUUID(),
            userId = userId,
            activityState = state ?: "",
            createdAt = Instant.now()
        )

        logger.debug { "Setting current state for user $userId to ${entity.activityState}" }

        return repository
            .save(entity)
            .let { Either.Right(Unit) }
            .tap { logger.debug("Activity state for user $userId has been updated") }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}