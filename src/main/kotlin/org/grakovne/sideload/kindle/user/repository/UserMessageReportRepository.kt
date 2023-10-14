package org.grakovne.sideload.kindle.user.repository

import org.grakovne.sideload.kindle.user.domain.UserMessageReport
import org.springframework.data.jpa.repository.JpaRepository

interface UserMessageReportRepository : JpaRepository<UserMessageReport, String>