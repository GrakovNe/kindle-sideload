package org.grakovne.sideload.kindle.converter.task.repository

import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID


interface ConvertationTaskRepository : JpaRepository<ConvertationTask, UUID> {

    fun findByCreatedAtGreaterThanAndCreatedAtLessThan(
        from: Instant,
        to: Instant
    ): List<ConvertationTask>

    fun findByStatusInAndCreatedAtLessThan(
        status: List<ConvertationTaskStatus>,
        lastModifiedAt: Instant
    ): List<ConvertationTask>

}