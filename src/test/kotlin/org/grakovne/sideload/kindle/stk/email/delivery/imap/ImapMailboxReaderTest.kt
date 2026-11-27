package org.grakovne.sideload.kindle.stk.email.delivery.imap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ImapMailboxReaderTest {

    // Where the scan starts

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

    // How each message failure is treated

    @Test
    fun `handles every readable message and reports the highest uid`() {
        val handled = mutableListOf<Long>()

        val last = ImapMailboxReader.drain(
            read = listOf(11L to message(11), 12L to message(12)).asSequence(),
            lastProcessed = 10L,
            handler = { handled += it.uid },
        )

        assertEquals(listOf(11L, 12L), handled)
        assertEquals(12L, last)
    }

    @Test
    fun `skips an undecodable message and still moves past it`() {
        val handled = mutableListOf<Long>()

        val last = ImapMailboxReader.drain(
            read = listOf(11L to null, 12L to message(12)).asSequence(),
            lastProcessed = 10L,
            handler = { handled += it.uid },
        )

        // the poison message must not block the later bounce, nor the baseline
        assertEquals(listOf(12L), handled)
        assertEquals(12L, last)
    }

    @Test
    fun `a poison message at the head does not stall the scan`() {
        val handled = mutableListOf<Long>()

        val last = ImapMailboxReader.drain(
            read = listOf(11L to null, 12L to message(12), 13L to message(13)).asSequence(),
            lastProcessed = 10L,
            handler = { handled += it.uid },
        )

        assertEquals(listOf(12L, 13L), handled)
        assertEquals(13L, last)
    }

    @Test
    fun `a handler failure propagates so the baseline is not advanced`() {
        assertThrows(RuntimeException::class.java) {
            ImapMailboxReader.drain(
                read = listOf(11L to message(11), 12L to message(12)).asSequence(),
                lastProcessed = 10L,
                handler = { throw RuntimeException("database exploded") },
            )
        }
    }

    @Test
    fun `stops reading messages after a handler failure`() {
        var readCount = 0
        assertThrows(RuntimeException::class.java) {
            ImapMailboxReader.drain(
                read = sequence {
                    readCount++
                    yield(11L to message(11))
                    readCount++
                    yield(12L to message(12))
                },
                lastProcessed = 10L,
                handler = { throw RuntimeException("database exploded") },
            )
        }

        assertEquals(1, readCount)
    }

    @Test
    fun `ignores messages at or below the stored position`() {
        val handled = mutableListOf<Long>()

        val last = ImapMailboxReader.drain(
            read = listOf(10L to message(10), 11L to message(11)).asSequence(),
            lastProcessed = 10L,
            handler = { handled += it.uid },
        )

        assertEquals(listOf(11L), handled)
        assertEquals(11L, last)
    }

    @Test
    fun `reports the stored position when there is nothing new`() {
        val last = ImapMailboxReader.drain(read = emptySequence(), lastProcessed = 42L, handler = { })

        assertEquals(42L, last)
    }

    private fun message(uid: Long) = RawEmailMessage(
        folder = "INBOX",
        uid = uid,
        subject = "Undelivered Mail Returned to Sender",
        body = "Status: 5.7.1",
    )
}
