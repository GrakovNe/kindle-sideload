package org.grakovne.sideload.kindle.user.message.report.repository

import org.grakovne.sideload.kindle.user.message.report.domain.UserMessageReport
import org.springframework.data.jpa.repository.JpaRepository

interface UserMessageReportRepository : JpaRepository<UserMessageReport, String>