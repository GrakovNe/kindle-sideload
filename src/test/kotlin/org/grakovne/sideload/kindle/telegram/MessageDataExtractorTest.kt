package org.grakovne.sideload.kindle.telegram

import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.ChatMemberUpdated
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.User
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MessageDataExtractorTest {

    private val update = mock<Update>()
    private val message = mock<Message>()
    private val callbackQuery = mock<CallbackQuery>()
    private val callbackMessage = mock<Message>()
    private val chatMember = mock<ChatMemberUpdated>()
    private val chat = mock<Chat>()
    private val from = mock<User>()
    private val callbackFrom = mock<User>()

    private fun callbackQueryWithMessage(): CallbackQuery {
        whenever(callbackQuery.message()).thenReturn(callbackMessage)
        return callbackQuery
    }

    @Test
    fun `fetches the unique identifier from the message`() {
        whenever(update.message()).thenReturn(message)
        whenever(message.messageId()).thenReturn(42)

        assertEquals("42", update.fetchUniqueIdentifier())
    }

    @Test
    fun `fetches the unique identifier from the callback query message`() {
        whenever(update.message()).thenReturn(null)
        val callbackWithMessage = callbackQueryWithMessage()
        whenever(update.callbackQuery()).thenReturn(callbackWithMessage)
        whenever(callbackMessage.messageId()).thenReturn(7)

        assertEquals("7", update.fetchUniqueIdentifier())
    }

    @Test
    fun `falls back to a random uuid when there is no message and no callback`() {
        whenever(update.message()).thenReturn(null)
        whenever(update.callbackQuery()).thenReturn(null)

        val id = update.fetchUniqueIdentifier()

        assertTrue(id.matches(Regex("[0-9a-f-]{36}")), "expected a uuid, got $id")
    }

    @Test
    fun `fetches the user id from the my chat member`() {
        whenever(update.myChatMember()).thenReturn(chatMember)
        whenever(chatMember.chat()).thenReturn(chat)
        whenever(chat.id()).thenReturn(5L)

        assertEquals("5", update.fetchUserId())
    }

    @Test
    fun `fetches the user id from the message chat`() {
        whenever(update.myChatMember()).thenReturn(null)
        whenever(update.message()).thenReturn(message)
        whenever(message.chat()).thenReturn(chat)
        whenever(chat.id()).thenReturn(11L)

        assertEquals("11", update.fetchUserId())
    }

    @Test
    fun `fetches the user id from the message sender when the chat id is absent`() {
        whenever(update.myChatMember()).thenReturn(null)
        whenever(update.message()).thenReturn(message)
        whenever(message.chat()).thenReturn(chat)
        whenever(chat.id()).thenReturn(null)
        whenever(message.from()).thenReturn(from)
        whenever(from.id()).thenReturn(22L)

        assertEquals("22", update.fetchUserId())
    }

    @Test
    fun `fetches the user id from the callback query chat`() {
        whenever(update.myChatMember()).thenReturn(null)
        whenever(update.message()).thenReturn(null)
        val callbackWithMessage = callbackQueryWithMessage()
        whenever(update.callbackQuery()).thenReturn(callbackWithMessage)
        whenever(callbackMessage.chat()).thenReturn(chat)
        whenever(chat.id()).thenReturn(33L)

        assertEquals("33", update.fetchUserId())
    }

    @Test
    fun `fetches the user id from the callback query sender when the chat id is absent`() {
        whenever(update.myChatMember()).thenReturn(null)
        whenever(update.message()).thenReturn(null)
        val callbackWithMessage = callbackQueryWithMessage()
        whenever(update.callbackQuery()).thenReturn(callbackWithMessage)
        whenever(callbackMessage.chat()).thenReturn(chat)
        whenever(chat.id()).thenReturn(null)
        whenever(callbackQuery.from()).thenReturn(callbackFrom)
        whenever(callbackFrom.id()).thenReturn(44L)

        assertEquals("44", update.fetchUserId())
    }

    @Test
    fun `throws when the user id cannot be extracted`() {
        whenever(update.myChatMember()).thenReturn(null)
        whenever(update.message()).thenReturn(null)
        whenever(update.callbackQuery()).thenReturn(null)

        assertFailsWith<IllegalArgumentException> { update.fetchUserId() }
    }

    @Test
    fun `fetches the language from the message sender`() {
        whenever(update.message()).thenReturn(message)
        whenever(message.from()).thenReturn(from)
        whenever(from.languageCode()).thenReturn("ru")

        assertEquals("ru", update.fetchLanguage())
    }

    @Test
    fun `defaults to english when the message sender has no language`() {
        whenever(update.message()).thenReturn(message)
        whenever(message.from()).thenReturn(from)
        whenever(from.languageCode()).thenReturn(null)

        assertEquals("en", update.fetchLanguage())
    }

    @Test
    fun `fetches the language from the callback query sender`() {
        whenever(update.message()).thenReturn(null)
        val callbackWithMessage = callbackQueryWithMessage()
        whenever(update.callbackQuery()).thenReturn(callbackWithMessage)
        whenever(callbackQuery.from()).thenReturn(callbackFrom)
        whenever(callbackFrom.languageCode()).thenReturn("ru")

        assertEquals("ru", update.fetchLanguage())
    }

    @Test
    fun `defaults to english when there is no message and no callback`() {
        whenever(update.message()).thenReturn(null)
        whenever(update.callbackQuery()).thenReturn(null)

        assertEquals("en", update.fetchLanguage())
    }
}
