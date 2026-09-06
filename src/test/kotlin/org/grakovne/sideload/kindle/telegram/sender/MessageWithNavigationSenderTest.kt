package org.grakovne.sideload.kindle.telegram.sender

import arrow.core.Either
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import org.grakovne.sideload.kindle.common.navigation.domain.Button.Companion.buildQualifiedName
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.telegram.ConfigurationProperties
import org.grakovne.sideload.kindle.telegram.domain.PreparedButton
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.sideload.kindle.telegram.domain.error.UnableSendResponse
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestProjectInfoButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestSettingButton
import org.grakovne.sideload.kindle.telegram.localization.LocalizationError
import org.grakovne.sideload.kindle.telegram.localization.MessageLocalizationService
import org.grakovne.sideload.kindle.telegram.localization.NavigationLocalizationService
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.pengrad.telegrambot.model.Message as TelegramMessage
import org.grakovne.sideload.kindle.telegram.domain.error.LocalizationError as SendError

class MessageWithNavigationSenderTest {

    private val responseSender = mock<ResponseSender>()
    private val navigationLocalizationService = mock<NavigationLocalizationService>()
    private val messageLocalizationService = mock<MessageLocalizationService>()
    private val configurationProperties = ConfigurationProperties()

    private lateinit var sut: MessageWithNavigationSender

    @BeforeEach
    fun setUp() {
        sut = MessageWithNavigationSender(
            responseSender,
            navigationLocalizationService,
            messageLocalizationService,
            configurationProperties
        )
    }

    @Test
    fun `sends the localized message with the inline keyboard`() {
        val message = object : Message {}
        whenever(messageLocalizationService.localize(eq(message), eq("ru")))
            .thenReturn(Either.Right(PreparedMessage("Привет", true)))
        whenever(navigationLocalizationService.localize(eq(RequestSettingButton), eq("ru")))
            .thenReturn(Either.Right(PreparedButton("Настройки", "RequestSettingButton")))
        whenever(navigationLocalizationService.localize(eq(RequestProjectInfoButton), eq("ru")))
            .thenReturn(Either.Right(PreparedButton("О проекте", "RequestProjectInfoButton")))
        whenever(responseSender.sendMessage(any<SendMessage>())).thenReturn(Either.Right(Unit))

        val result = sut.sendResponse(
            "chat-1",
            user(language = "ru"),
            message,
            listOf(listOf(RequestSettingButton, RequestProjectInfoButton))
        )

        assertEquals(Either.Right(Unit), result)
        val captor = argumentCaptor<SendMessage>()
        verify(responseSender).sendMessage(captor.capture())
        val sent = captor.firstValue
        assertEquals("Привет", sent.text)
        assertEquals(ParseMode.HTML, sent.parseMode)
        val markup = sent.replyMarkup
        assertTrue(markup is InlineKeyboardMarkup, "expected an inline keyboard, got ${markup?.let { it.javaClass.simpleName }}")
        val rows = markup.inlineKeyboard()
        assertEquals(1, rows.size)
        assertEquals(2, rows[0].size)
        assertEquals("Настройки", rows[0][0].text)
        assertEquals(RequestSettingButton.buildQualifiedName(), rows[0][0].callbackData)
        assertEquals("О проекте", rows[0][1].text)
        assertEquals(RequestProjectInfoButton.buildQualifiedName(), rows[0][1].callbackData)
    }

    @Test
    fun `sends the message with the keyboard removal when there is no navigation`() {
        val message = object : Message {}
        whenever(messageLocalizationService.localize(eq(message), eq("en")))
            .thenReturn(Either.Right(PreparedMessage("Hi", false)))
        whenever(responseSender.sendMessage(any<SendMessage>())).thenReturn(Either.Right(Unit))

        val result = sut.sendResponse("chat-1", user(), message, emptyList())

        assertEquals(Either.Right(Unit), result)
        val captor = argumentCaptor<SendMessage>()
        verify(responseSender).sendMessage(captor.capture())
        val sent = captor.firstValue
        assertEquals("Hi", sent.text)
        val markup = sent.replyMarkup
        assertTrue(markup is ReplyKeyboardRemove, "expected a keyboard removal, got ${markup?.let { it.javaClass.simpleName }}")
        assertTrue(sent.linkPreviewOptions!!.isDisabled)
    }

    @Test
    fun `filters out the empty keyboard rows before localisation`() {
        val message = object : Message {}
        whenever(messageLocalizationService.localize(eq(message), eq("en")))
            .thenReturn(Either.Right(PreparedMessage("Hi", true)))
        whenever(navigationLocalizationService.localize(eq(RequestSettingButton), eq("en")))
            .thenReturn(Either.Right(PreparedButton("Settings", "RequestSettingButton")))
        whenever(responseSender.sendMessage(any<SendMessage>())).thenReturn(Either.Right(Unit))

        val result = sut.sendResponse(
            "chat-1",
            user(),
            message,
            listOf(emptyList(), listOf(RequestSettingButton))
        )

        assertEquals(Either.Right(Unit), result)
        val captor = argumentCaptor<SendMessage>()
        verify(responseSender).sendMessage(captor.capture())
        val markup = captor.firstValue.replyMarkup
        assertTrue(markup is InlineKeyboardMarkup)
        assertEquals(1, markup.inlineKeyboard().size)
        assertEquals(1, markup.inlineKeyboard()[0].size)
    }

    @Test
    fun `reports the localisation error when the message cannot be localized`() {
        val message = object : Message {}
        whenever(messageLocalizationService.localize(eq(message), eq("ru")))
            .thenReturn(Either.Left(LocalizationError.TEMPLATE_NOT_FOUND))
        whenever(responseSender.sendMessage(any<SendMessage>())).thenReturn(Either.Right(Unit))

        val result = sut.sendResponse("chat-1", user(language = "ru"), message, emptyList())

        assertEquals(Either.Left(SendError), result)
        verify(responseSender, never()).sendMessage(any<SendMessage>())
    }

    @Test
    fun `reports the localisation error when the navigation cannot be localized`() {
        val message = object : Message {}
        whenever(messageLocalizationService.localize(eq(message), eq("ru")))
            .thenReturn(Either.Right(PreparedMessage("Привет", true)))
        whenever(navigationLocalizationService.localize(eq(RequestSettingButton), eq("ru")))
            .thenReturn(Either.Left(LocalizationError.TEMPLATE_NOT_FOUND))
        whenever(responseSender.sendMessage(any<SendMessage>())).thenReturn(Either.Right(Unit))

        val result = sut.sendResponse(
            "chat-1",
            user(language = "ru"),
            message,
            listOf(listOf(RequestSettingButton))
        )

        assertEquals(Either.Left(SendError), result)
        verify(responseSender, never()).sendMessage(any<SendMessage>())
    }

    @Test
    fun `edits the pressed message when interactive editing is enabled`() {
        configurationProperties.interactiveMessageEditing = true
        val message = givenMessage("Новый текст")
        whenever(responseSender.editMessage(any<EditMessageText>())).thenReturn(Either.Right(Unit))

        val result = sut.sendResponse(
            callbackUpdate(pressedTextMessage()),
            user(),
            message,
            listOf(listOf(RequestSettingButton))
        )

        assertEquals(Either.Right(Unit), result)
        val captor = argumentCaptor<EditMessageText>()
        verify(responseSender).editMessage(captor.capture())
        verify(responseSender, never()).sendMessage(any<SendMessage>())
        val edited = captor.firstValue.getParameters()
        assertEquals("100", edited["chat_id"])
        assertEquals(7, edited["message_id"])
        assertEquals("Новый текст", edited["text"])
        assertEquals("HTML", edited["parse_mode"].toString())
        val markup = edited["reply_markup"]
        assertTrue(markup is InlineKeyboardMarkup)
        assertEquals(RequestSettingButton.buildQualifiedName(), markup.inlineKeyboard()[0][0].callbackData)
    }

    @Test
    fun `sends a new message when interactive editing is disabled`() {
        configurationProperties.interactiveMessageEditing = false
        val message = givenMessage("Текст")
        whenever(responseSender.sendMessage(any<SendMessage>())).thenReturn(Either.Right(Unit))

        val result = sut.sendResponse(
            callbackUpdate(pressedTextMessage()),
            user(),
            message,
            listOf(listOf(RequestSettingButton))
        )

        assertEquals(Either.Right(Unit), result)
        verify(responseSender, never()).editMessage(any<EditMessageText>())
        verify(responseSender).sendMessage(any<SendMessage>())
    }

    @Test
    fun `sends a new message when the update is not a callback`() {
        configurationProperties.interactiveMessageEditing = true
        val message = givenMessage("Текст")
        whenever(responseSender.sendMessage(any<SendMessage>())).thenReturn(Either.Right(Unit))

        val plainMessage = mock<TelegramMessage>()
        val plainMessageChat = chatWithId(100L)
        whenever(plainMessage.chat()).thenReturn(plainMessageChat)
        val update = mock<Update>()
        whenever(update.myChatMember()).thenReturn(null)
        whenever(update.message()).thenReturn(plainMessage)
        whenever(update.callbackQuery()).thenReturn(null)

        val result = sut.sendResponse(update, user(), message, listOf(listOf(RequestSettingButton)))

        assertEquals(Either.Right(Unit), result)
        verify(responseSender, never()).editMessage(any<EditMessageText>())
        verify(responseSender).sendMessage(any<SendMessage>())
    }

    @Test
    fun `sends a new message when the pressed message is not a text one`() {
        configurationProperties.interactiveMessageEditing = true
        val message = givenMessage("Текст")
        whenever(responseSender.sendMessage(any<SendMessage>())).thenReturn(Either.Right(Unit))

        val pressedMessage = mock<TelegramMessage>()
        whenever(pressedMessage.text()).thenReturn(null)
        val pressedMessageChat = chatWithId(100L)
        whenever(pressedMessage.chat()).thenReturn(pressedMessageChat)

        val result = sut.sendResponse(callbackUpdate(pressedMessage), user(), message, listOf(listOf(RequestSettingButton)))

        assertEquals(Either.Right(Unit), result)
        verify(responseSender, never()).editMessage(any<EditMessageText>())
        verify(responseSender).sendMessage(any<SendMessage>())
    }

    @Test
    fun `falls back to a new message when the edit fails`() {
        configurationProperties.interactiveMessageEditing = true
        val message = givenMessage("Текст")
        whenever(responseSender.editMessage(any<EditMessageText>())).thenReturn(Either.Left(UnableSendResponse))
        whenever(responseSender.sendMessage(any<SendMessage>())).thenReturn(Either.Right(Unit))

        val result = sut.sendResponse(
            callbackUpdate(pressedTextMessage()),
            user(),
            message,
            listOf(listOf(RequestSettingButton))
        )

        assertEquals(Either.Right(Unit), result)
        verify(responseSender).editMessage(any<EditMessageText>())
        verify(responseSender).sendMessage(any<SendMessage>())
    }

    @Test
    fun `reports the failure when both the edit and the fallback send fail`() {
        configurationProperties.interactiveMessageEditing = true
        val message = givenMessage("Текст")
        whenever(responseSender.editMessage(any<EditMessageText>())).thenReturn(Either.Left(UnableSendResponse))
        whenever(responseSender.sendMessage(any<SendMessage>())).thenReturn(Either.Left(UnableSendResponse))

        val result = sut.sendResponse(
            callbackUpdate(pressedTextMessage()),
            user(),
            message,
            listOf(listOf(RequestSettingButton))
        )

        assertEquals(Either.Left(UnableSendResponse), result)
        verify(responseSender).editMessage(any<EditMessageText>())
        verify(responseSender).sendMessage(any<SendMessage>())
    }

    @Test
    fun `clears the keyboard when editing without navigation`() {
        configurationProperties.interactiveMessageEditing = true
        val message = givenMessage("Текст")
        whenever(responseSender.editMessage(any<EditMessageText>())).thenReturn(Either.Right(Unit))

        val result = sut.sendResponse(callbackUpdate(pressedTextMessage()), user(), message, emptyList())

        assertEquals(Either.Right(Unit), result)
        val captor = argumentCaptor<EditMessageText>()
        verify(responseSender).editMessage(captor.capture())
        val markup = captor.firstValue.getParameters()["reply_markup"]
        assertTrue(markup is InlineKeyboardMarkup, "expected an empty inline keyboard, got ${markup?.let { it.javaClass.simpleName }}")
        assertEquals(0, markup.inlineKeyboard().size)
    }

    private fun givenMessage(text: String): Message {
        val message = object : Message {}
        whenever(messageLocalizationService.localize(eq(message), any()))
            .thenReturn(Either.Right(PreparedMessage(text, true)))
        whenever(navigationLocalizationService.localize(eq(RequestSettingButton), any()))
            .thenReturn(Either.Right(PreparedButton("Настройки", "RequestSettingButton")))
        return message
    }

    private fun pressedTextMessage(): TelegramMessage {
        val pressedMessage = mock<TelegramMessage>()
        whenever(pressedMessage.text()).thenReturn("старый текст")
        whenever(pressedMessage.messageId()).thenReturn(7)
        val pressedMessageChat = chatWithId(100L)
        whenever(pressedMessage.chat()).thenReturn(pressedMessageChat)
        return pressedMessage
    }

    private fun callbackUpdate(pressedMessage: TelegramMessage): Update {
        val callbackQuery = mock<CallbackQuery>()
        whenever(callbackQuery.message()).thenReturn(pressedMessage)
        whenever(callbackQuery.id()).thenReturn("callback-1")

        val update = mock<Update>()
        whenever(update.myChatMember()).thenReturn(null)
        whenever(update.message()).thenReturn(null)
        whenever(update.callbackQuery()).thenReturn(callbackQuery)
        return update
    }

    private fun chatWithId(id: Long): Chat {
        val chat = mock<Chat>()
        whenever(chat.id()).thenReturn(id)
        return chat
    }

    private fun user(language: String = "en") = User(
        id = "user-1",
        language = language,
        type = Type.FREE_USER,
        lastActivityTimestamp = null
    )
}
