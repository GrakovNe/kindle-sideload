package org.grakovne.sideload.kindle.user.message.report.repository

import org.grakovne.sideload.kindle.user.message.report.domain.UserMessageReport
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface UserMessageReportRepository : JpaRepository<UserMessageReport, String> {

    fun findByCreatedAtGreaterThanAndCreatedAtLessThan(
        from: Instant,
        to: Instant
    ): List<UserMessageReport>
}