package org.grakovne.sideload.kindle.telegram.localization.converter

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class InstantFormatterTest {

    @Test
    fun `formats the instant in the fixed user message format in utc`() {
        assertEquals("01.08.2026 12:34:56", Instant.parse("2026-08-01T12:34:56Z").toMessage())
    }

    @Test
    fun `keeps the utc time when formatting an instant in a distant zone`() {
        assertEquals("16.01.2026 23:00:00", Instant.parse("2026-01-16T23:00:00Z").toMessage())
    }
}
