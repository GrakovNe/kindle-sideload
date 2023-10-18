package org.grakovne.sideload.kindle.converter.task.repository

import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converter.task.domain.ConvertationTaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID


interface ConvertationTaskRepository : JpaRepository<ConvertationTask, UUID> {

    fun findByStatusInAndCreatedAtLessThan(
        status: List<ConvertationTaskStatus>,
        lastModifiedAt: Instant
    ): List<ConvertationTask>

}