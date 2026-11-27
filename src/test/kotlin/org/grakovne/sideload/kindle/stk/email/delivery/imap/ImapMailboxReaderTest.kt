package org.grakovne.sideload.kindle.stk.email.delivery.imap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImapMailboxReaderTest {

    @Test
    fun `continues above the stored position`() {
        val baseline = ScanBaseline(uidValidity = 7L, lastProcessedUid = 100L)

        assertEquals(100L, ImapMailboxReader.startUid(baseline, uidValidity = 7L, uidNext = 150L, backfillHistory = false))
    }

    @Test
    fun `discards a baseline from another uid epoch`() {
        val baseline = ScanBaseline(uidValidity = 7L, lastProcessedUid = 100L)

        // recreated folder: start over at the newest message, not at the stale uid
        assertEquals(149L, ImapMailboxReader.startUid(baseline, uidValidity = 8L, uidNext = 150L, backfillHistory = false))
    }

    @Test
    fun `first scan starts past the newest message`() {
        assertEquals(149L, ImapMailboxReader.startUid(null, uidValidity = 7L, uidNext = 150L, backfillHistory = false))
    }

    @Test
    fun `backfill starts at the first message`() {
        assertEquals(0L, ImapMailboxReader.startUid(null, uidValidity = 7L, uidNext = 150L, backfillHistory = true))
    }

    @Test
    fun `backfill of an empty mailbox starts at zero`() {
        assertEquals(0L, ImapMailboxReader.startUid(null, uidValidity = 7L, uidNext = 1L, backfillHistory = false))
    }
}
