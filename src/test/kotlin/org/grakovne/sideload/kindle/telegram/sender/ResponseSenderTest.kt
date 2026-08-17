package org.grakovne.sideload.kindle.telegram.sender

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import org.grakovne.sideload.kindle.telegram.domain.error.UnableSendResponse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class ResponseSenderTest {

    private val bot = mock<TelegramBot>()
    private val sut = ResponseSender(bot)

    @Test
    fun `reports the success when the bot accepted the message`() {
        val response = mock<SendResponse>()
        whenever(response.isOk).thenReturn(true)
        whenever(bot.execute(any<SendMessage>())).thenReturn(response)

        val result = sut.sendMessage(SendMessage("chat-1", "hello"))

        assertEquals(Either.Right(Unit), result)
    }

    @Test
    fun `reports the failure when the bot rejected the message`() {
        val response = mock<SendResponse>()
        whenever(response.isOk).thenReturn(false)
        whenever(bot.execute(any<SendMessage>())).thenReturn(response)

        val result = sut.sendMessage(SendMessage("chat-1", "hello"))

        assertEquals(Either.Left(UnableSendResponse), result)
    }
}
