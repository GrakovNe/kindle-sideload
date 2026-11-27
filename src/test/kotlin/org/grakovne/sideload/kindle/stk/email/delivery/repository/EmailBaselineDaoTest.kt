package org.grakovne.sideload.kindle.stk.email.delivery.repository

import org.grakovne.sideload.kindle.TestDatabase
import org.grakovne.sideload.kindle.stk.email.delivery.domain.EmailBaseline
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class EmailBaselineDaoTest : TestDatabase() {

    @Autowired
    private lateinit var emailBaselineDao: EmailBaselineDao

    @Test
    fun `has no baseline for an unknown folder`() {
        assertNull(emailBaselineDao.find("INBOX"))
    }

    @Test
    fun `stores and retrieves a baseline`() {
        emailBaselineDao.save(baseline())

        val stored = emailBaselineDao.find("INBOX")!!
        assertEquals(7L, stored.uidValidity)
        assertEquals(42L, stored.lastProcessedUid)
    }

    @Test
    fun `overwrites the position on save`() {
        emailBaselineDao.save(baseline(lastProcessedUid = 42L))
        emailBaselineDao.save(baseline(lastProcessedUid = 99L))

        assertEquals(99L, emailBaselineDao.find("INBOX")!!.lastProcessedUid)
    }

    @Test
    fun `overwrites the uid epoch on save`() {
        emailBaselineDao.save(baseline(uidValidity = 7L, lastProcessedUid = 500L))
        emailBaselineDao.save(baseline(uidValidity = 8L, lastProcessedUid = 3L))

        val stored = emailBaselineDao.find("INBOX")!!
        assertEquals(8L, stored.uidValidity)
        assertEquals(3L, stored.lastProcessedUid)
    }

    @Test
    fun `keeps one baseline per folder`() {
        emailBaselineDao.save(baseline(imapFolder = "INBOX", lastProcessedUid = 11L))
        emailBaselineDao.save(baseline(imapFolder = "Junk", lastProcessedUid = 22L))

        assertEquals(11L, emailBaselineDao.find("INBOX")!!.lastProcessedUid)
        assertEquals(22L, emailBaselineDao.find("Junk")!!.lastProcessedUid)
    }

    private fun baseline(
        imapFolder: String = "INBOX",
        uidValidity: Long = 7L,
        lastProcessedUid: Long = 42L,
    ) = EmailBaseline(
        imapFolder = imapFolder,
        uidValidity = uidValidity,
        lastProcessedUid = lastProcessedUid,
        updatedAt = LocalDateTime.now(),
    )
}
