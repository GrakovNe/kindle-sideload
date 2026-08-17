package org.grakovne.sideload.kindle.telegram.message.reference.service

import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageReference
import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageStatus
import org.grakovne.sideload.kindle.telegram.message.reference.repository.MessageReferenceRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

@DataJpaTest
class MessageReferenceServiceTest {

    @Autowired
    lateinit var repository: MessageReferenceRepository

    private lateinit var sut: MessageReferenceService

    @BeforeEach
    fun setUp() {
        sut = MessageReferenceService(repository)
    }

    @Test
    fun `returns null when the message is not found`() {
        assertNull(sut.fetchMessage("missing-id"))
    }

    @Test
    fun `returns the stored reference when the message exists`() {
        repository.save(MessageReference("msg-1", MessageStatus.UNKNOWN))

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
        assertEquals(1, repository.count())
    }
}
