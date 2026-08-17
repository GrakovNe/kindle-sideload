package org.grakovne.sideload.kindle.user.message.report.service

import org.grakovne.sideload.kindle.user.message.report.repository.UserMessageReportRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import kotlin.test.assertEquals

@DataJpaTest
class UserMessageReportServiceTest {

    @Autowired
    lateinit var repository: UserMessageReportRepository

    private lateinit var sut: UserMessageReportService

    @BeforeEach
    fun setUp() {
        sut = UserMessageReportService(repository)
    }

    @Test
    fun `stores a report entry with the user and the text`() {
        val report = sut.createReportEntry("user-1", "the book looks great")

        assertEquals("user-1", report.userId)
        assertEquals("the book looks great", report.text)
        assertEquals(1L, repository.count())
    }

    @Test
    fun `stores a report entry with a null text`() {
        val report = sut.createReportEntry("user-1", null)

        assertEquals(null, report.text)
        assertEquals("user-1", report.userId)
    }
}
