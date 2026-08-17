package org.grakovne.sideload.kindle.telegram.sender

import arrow.core.Either
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove
import com.pengrad.telegrambot.request.SendMessage
import org.grakovne.sideload.kindle.common.navigation.domain.Button.Companion.buildQualifiedName
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.telegram.domain.PreparedButton
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestProjectInfoButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestSettingButton
import org.grakovne.sideload.kindle.telegram.domain.error.LocalizationError as SendError
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

class MessageWithNavigationSenderTest {

    private val responseSender = mock<ResponseSender>()
    private val navigationLocalizationService = mock<NavigationLocalizationService>()
    private val messageLocalizationService = mock<MessageLocalizationService>()

    private lateinit var sut: MessageWithNavigationSender

    @BeforeEach
    fun setUp() {
        sut = MessageWithNavigationSender(responseSender, navigationLocalizationService, messageLocalizationService)
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

    private fun user(language: String = "en") = User(
        id = "user-1",
        language = language,
        type = Type.FREE_USER,
        lastActivityTimestamp = null
    )
}
