package org.grakovne.sideload.kindle.converer.task.repository

import org.grakovne.sideload.kindle.converer.task.domain.ConvertationTask
import org.grakovne.sideload.kindle.converer.task.domain.ConvertationTaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.*


interface ConvertationTaskRepository : JpaRepository<ConvertationTask, UUID> {

    fun findByStatusInAndCreatedAtLessThan(
        status: List<ConvertationTaskStatus>,
        lastModifiedAt: Instant
    ): List<ConvertationTask>

}