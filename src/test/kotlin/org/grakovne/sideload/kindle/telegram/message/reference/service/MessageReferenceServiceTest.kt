package org.grakovne.sideload.kindle.telegram.message.reference.service

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageReference
import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageStatus
import org.grakovne.sideload.kindle.telegram.message.reference.repository.MessageReferenceDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class MessageReferenceServiceTest : TestDatabase() {

    @Autowired
    lateinit var dao: MessageReferenceDao

    private lateinit var sut: MessageReferenceService

    @BeforeEach
    fun setUp() {
        sut = MessageReferenceService(dao)
    }

    @Test
    fun `returns null when the message is not found`() {
        assertNull(sut.fetchMessage("missing-id"))
    }

    @Test
    fun `returns the stored reference when the message exists`() {
        dao.save(MessageReference("msg-1", MessageStatus.UNKNOWN))

        val reference = sut.fetchMessage("msg-1")

        assertEquals("msg-1", reference?.id)
        assertEquals(MessageStatus.UNKNOWN, reference?.status)
    }

    @Test
    fun `persists a processed reference for the message`() {
        val result = sut.markAsProcessed("msg-1")

        assertEquals("msg-1", result.id)
        assertEquals(MessageStatus.PROCESSED, result.status)

        val stored = sut.fetchMessage("msg-1")
        assertSame(MessageStatus.PROCESSED, stored?.status)
        assertEquals(1, dao.count())
    }
}
