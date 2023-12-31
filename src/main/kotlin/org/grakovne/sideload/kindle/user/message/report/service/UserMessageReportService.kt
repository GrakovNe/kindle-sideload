package org.grakovne.sideload.kindle.user.message.report.service

import mu.KotlinLogging
import org.grakovne.sideload.kindle.user.message.report.domain.UserMessageReport
import org.grakovne.sideload.kindle.user.message.report.repository.UserMessageReportRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class UserMessageReportService(private val repository: UserMessageReportRepository) {

    fun createReportEntry(userId: String, text: String?) =
        UserMessageReport(
            id = UUID.randomUUID(),
            userId = userId,
            createdAt = Instant.now(),
            text = text
        ).let(repository::save)

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}