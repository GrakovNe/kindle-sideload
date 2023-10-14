package org.grakovne.sideload.kindle.user

import org.grakovne.sideload.kindle.user.domain.UserMessageReport
import org.grakovne.sideload.kindle.user.repository.UserMessageReportRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UserMessageReportService(private val repository: UserMessageReportRepository) {

    fun createReportEntry(userId: String, text: String?) =
        UserMessageReport(
            id = UUID.randomUUID(),
            userId = userId,
            createdAt = Instant.now(),
            text = text
        ).let(repository::save)
}