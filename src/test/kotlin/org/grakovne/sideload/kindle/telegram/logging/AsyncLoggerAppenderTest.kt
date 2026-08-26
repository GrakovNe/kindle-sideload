package org.grakovne.sideload.kindle.telegram.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import org.grakovne.sideload.kindle.telegram.ConfigurationProperties
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class AsyncLoggerAppenderTest {

    private val bot: TelegramBot = mock()
    private val userService: UserService = mock()
    private val superUser = User("super-1", "en", Type.SUPER_USER, null)

    private fun configuration(level: Level, loggingTimeout: Duration): ConfigurationProperties =
        ConfigurationProperties().apply {
            token = "token"
            this.level = level
            this.loggingTimeout = loggingTimeout
        }

    // The flush window is already elapsed, so the first qualifying event is flushed
    // to the super user on the appender's worker thread.
    @Test
    fun `queues a qualifying event and flushes it to the super user after the window elapses`() {
        val appender = AsyncLoggerAppender(bot, userService, configuration(Level.ERROR, Duration.ofSeconds(-1)))
        whenever(userService.fetchSuperUsers()).thenReturn(listOf(superUser))

        deliver(appender, event(Level.ERROR, "disk almost full"))

        val sent = capturedSentMessages()
        assertEquals(1, sent.size)
        assertTrue("disk almost full" in sent[0].text)
        assertTrue(Level.ERROR.toString() in sent[0].text)
    }

    @Test
    fun `ignores events that are below the configured level`() {
        val appender = AsyncLoggerAppender(bot, userService, configuration(Level.ERROR, Duration.ofSeconds(-1)))
        whenever(userService.fetchSuperUsers()).thenReturn(listOf(superUser))

        deliver(appender, event(Level.INFO, "chatty message"))

        verify(userService, never()).fetchSuperUsers()
        verify(bot, never()).execute(any<SendMessage>())
    }

    @Test
    fun `sends nothing when there are no super users`() {
        val appender = AsyncLoggerAppender(bot, userService, configuration(Level.ERROR, Duration.ofSeconds(-1)))
        whenever(userService.fetchSuperUsers()).thenReturn(emptyList())

        deliver(appender, event(Level.ERROR, "discarded message"))

        verify(bot, never()).execute(any<SendMessage>())
    }

    @Test
    fun `sends nothing for a null event`() {
        val appender = AsyncLoggerAppender(bot, userService, configuration(Level.ERROR, Duration.ofSeconds(-1)))

        deliver(appender, null)

        verify(userService, never()).fetchSuperUsers()
        verify(bot, never()).execute(any<SendMessage>())
    }

    // The flush runs on the appender's single-thread executor; poll until the bot
    // records the send (or the deadline, which would mean the test is flaky).
    private fun capturedSentMessages(withinMs: Long = 5_000): List<SendMessage> {
        val deadline = System.currentTimeMillis() + withinMs
        while (System.currentTimeMillis() < deadline) {
            try {
                val captor = argumentCaptor<SendMessage>()
                verify(bot, atLeastOnce()).execute(captor.capture())
                return captor.allValues
            } catch (e: org.mockito.exceptions.verification.WantedButNotInvoked) {
                Thread.sleep(20)
            }
        }
        fail("expected the appender to flush a message within ${withinMs}ms")
    }

    private fun event(level: Level, message: String): ILoggingEvent {
        val logger = LoggerContext().getLogger("async-appender-test")
        return LoggingEvent("test", logger, level, message, null, null)
    }

    // [AppenderBase.doAppend] only dispatches to the protected [append] once the
    // appender has been started, so start it first and drive it through the public
    // logback entry point.
    private fun deliver(appender: AsyncLoggerAppender, event: ILoggingEvent?) {
        appender.start()
        appender.doAppend(event)
        appender.stop()
    }
}
