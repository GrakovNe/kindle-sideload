package org.grakovne.sideload.kindle.user.message.report.service

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.user.message.report.repository.UserMessageReportDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class UserMessageReportServiceTest : TestDatabase() {

    @Autowired
    lateinit var dao: UserMessageReportDao

    private lateinit var sut: UserMessageReportService

    @BeforeEach
    fun setUp() {
        sut = UserMessageReportService(dao)
    }

    @Test
    fun `stores a report entry with the user and the text`() {
        val report = sut.createReportEntry("user-1", "the book looks great")

        assertEquals("user-1", report.userId)
        assertEquals("the book looks great", report.text)
        assertEquals(1L, dao.count().toLong())
    }

    @Test
    fun `stores a report entry with a null text`() {
        val report = sut.createReportEntry("user-1", null)

        assertEquals(null, report.text)
        assertEquals("user-1", report.userId)
    }
}
