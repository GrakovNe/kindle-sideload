package org.grakovne.sideload.kindle.telegram.state.service

import arrow.core.Either
import org.grakovne.sideload.kindle.telegram.state.domain.ActivityState
import org.grakovne.sideload.kindle.telegram.state.domain.UserActivityState
import org.grakovne.sideload.kindle.telegram.state.repository.UserActivityStateRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UserActivityStateService(
    private val repository: UserActivityStateRepository
) {

    fun fetchCurrentState(userId: String): ActivityState? = repository
        .findByUserIdOrderByCreatedAtDesc(userId)
        .firstOrNull()
        ?.activityState

    fun setCurrentState(userId: String, state: ActivityState): Either<UserActivityStateError, Unit> {
        val entity = UserActivityState(
            id = UUID.randomUUID(),
            userId = userId,
            activityState = state,
            createdAt = Instant.now()
        )
        return repository.save(entity).let { Either.Right(Unit) }
    }

}