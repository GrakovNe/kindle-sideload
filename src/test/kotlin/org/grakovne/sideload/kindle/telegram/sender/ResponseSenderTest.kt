package org.grakovne.sideload.kindle.telegram.sender

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.BaseResponse
import com.pengrad.telegrambot.response.SendResponse
import org.grakovne.sideload.kindle.telegram.domain.error.UnableSendResponse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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

    @Test
    fun `reports the success when the bot accepted the edit`() {
        val response = mock<BaseResponse>()
        whenever(response.isOk).thenReturn(true)
        whenever(bot.execute(any<EditMessageText>())).thenReturn(response)

        val result = sut.editMessage(EditMessageText("chat-1", 7, "edited"))

        assertEquals(Either.Right(Unit), result)
    }

    @Test
    fun `treats the not-modified edit as a success`() {
        val response = mock<BaseResponse>()
        whenever(response.isOk).thenReturn(false)
        whenever(response.errorCode()).thenReturn(400)
        whenever(response.description()).thenReturn("Bad Request: message is not modified: specified new message content and reply markup are exactly the same as a current content and reply markup of the message")
        whenever(bot.execute(any<EditMessageText>())).thenReturn(response)

        val result = sut.editMessage(EditMessageText("chat-1", 7, "same"))

        assertEquals(Either.Right(Unit), result)
    }

    @Test
    fun `does not treat a different bad request as a not-modified success`() {
        val response = mock<BaseResponse>()
        whenever(response.isOk).thenReturn(false)
        whenever(response.errorCode()).thenReturn(400)
        whenever(response.description()).thenReturn("Bad Request: message to edit is not found")
        whenever(bot.execute(any<EditMessageText>())).thenReturn(response)

        val result = sut.editMessage(EditMessageText("chat-1", 7, "edited"))

        assertEquals(Either.Left(UnableSendResponse), result)
    }

    @Test
    fun `reports the failure when the bot rejected the edit`() {
        val response = mock<BaseResponse>()
        whenever(response.isOk).thenReturn(false)
        whenever(response.errorCode()).thenReturn(429)
        whenever(response.description()).thenReturn("Too Many Requests: retry after 30")
        whenever(bot.execute(any<EditMessageText>())).thenReturn(response)

        val result = sut.editMessage(EditMessageText("chat-1", 7, "edited"))

        assertEquals(Either.Left(UnableSendResponse), result)
    }

    @Test
    fun `answers the callback query`() {
        val response = mock<BaseResponse>()
        whenever(response.isOk).thenReturn(true)
        whenever(bot.execute(any<AnswerCallbackQuery>())).thenReturn(response)

        sut.answerCallbackQuery("callback-1")

        verify(bot).execute(any<AnswerCallbackQuery>())
    }

    @Test
    fun `swallows the callback query answer failure`() {
        val response = mock<BaseResponse>()
        whenever(response.isOk).thenReturn(false)
        whenever(response.description()).thenReturn("Bad Request: QUERY_ID_INVALID")
        whenever(bot.execute(any<AnswerCallbackQuery>())).thenReturn(response)

        sut.answerCallbackQuery("callback-1")
    }
}
