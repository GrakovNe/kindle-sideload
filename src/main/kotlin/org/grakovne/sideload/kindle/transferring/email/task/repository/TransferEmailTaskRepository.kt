package org.grakovne.sideload.kindle.transferring.email.task.repository

import org.grakovne.sideload.kindle.transferring.email.task.domain.TransferEmailTask
import org.grakovne.sideload.kindle.transferring.email.task.domain.TransferEmailTaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface TransferEmailTaskRepository : JpaRepository<TransferEmailTask, UUID> {

    fun findByStatusInAndCreatedAtLessThan(
        status: List<TransferEmailTaskStatus>,
        lastModifiedAt: Instant
    ): List<TransferEmailTask>
}