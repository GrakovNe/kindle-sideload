package org.grakovne.sideload.kindle.telegram.configuration

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventSender
import org.grakovne.sideload.kindle.telegram.ConfigurationProperties
import org.grakovne.sideload.kindle.telegram.handlers.UnprocessedIncomingEventService
import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageReference
import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageStatus
import org.grakovne.sideload.kindle.telegram.message.reference.service.MessageReferenceService
import org.grakovne.sideload.kindle.telegram.sender.ResponseSender
import org.grakovne.sideload.kindle.user.message.report.domain.UserMessageReport
import org.grakovne.sideload.kindle.user.message.report.service.UserMessageReportService
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class MessageListenersConfigurationTest {

    private val bot = mock<TelegramBot>()
    private val eventSender = mock<EventSender>()
    private val userService = mock<UserService>()
    private val userMessageReportService = mock<UserMessageReportService>()
    private val unprocessedIncomingEventService = mock<UnprocessedIncomingEventService>()
    private val messageReferenceService = mock<MessageReferenceService>()
    private val configurationProperties = ConfigurationProperties()
    private val responseSender = mock<ResponseSender>()

    private lateinit var listener: UpdatesListener

    @BeforeEach
    fun setUp() {
        MessageListenersConfiguration(
            bot = bot,
            eventSender = eventSender,
            userService = userService,
            userMessageReportService = userMessageReportService,
            unprocessedIncomingEventService = unprocessedIncomingEventService,
            messageReferenceService = messageReferenceService,
            configurationProperties = configurationProperties,
            responseSender = responseSender
        ).onCreate()

        val captor = argumentCaptor<UpdatesListener>()
        verify(bot).setUpdatesListener(captor.capture())
        listener = captor.firstValue

        whenever(userService.fetchOrCreateUser(any(), any())).thenReturn(user())
        whenever(userMessageReportService.createReportEntry(any(), anyOrNull()))
            .thenReturn(UserMessageReport(UUID.randomUUID(), "100", Instant.now(), "text"))
        whenever(eventSender.sendEvent(any())).thenReturn(listOf(Either.Right(EventProcessingResult.PROCESSED)))
    }

    @Test
    fun `answers the callback query and dispatches the event`() {
        whenever(messageReferenceService.fetchMessage("callback-1")).thenReturn(null)

        listener.process(listOf(callbackUpdate()))

        verify(responseSender).answerCallbackQuery("callback-1")
        verify(eventSender).sendEvent(any())
        verify(messageReferenceService).markAsProcessed("callback-1")
    }

    @Test
    fun `answers the callback query even when the update is a duplicate`() {
        whenever(messageReferenceService.fetchMessage("callback-1"))
            .thenReturn(MessageReference(id = "callback-1", status = MessageStatus.PROCESSED))

        listener.process(listOf(callbackUpdate()))

        verify(responseSender).answerCallbackQuery("callback-1")
        verify(eventSender, never()).sendEvent(any())
        verify(messageReferenceService, never()).markAsProcessed(any())
    }

    @Test
    fun `does not answer any callback query for a plain message`() {
        whenever(messageReferenceService.fetchMessage("5")).thenReturn(null)

        val message = mock<Message>()
        whenever(message.messageId()).thenReturn(5)
        val messageChat = chatWithId(100L)
        whenever(message.chat()).thenReturn(messageChat)
        whenever(message.text()).thenReturn("hello")

        val update = mock<Update>()
        whenever(update.myChatMember()).thenReturn(null)
        whenever(update.message()).thenReturn(message)
        whenever(update.callbackQuery()).thenReturn(null)

        listener.process(listOf(update))

        verify(responseSender, never()).answerCallbackQuery(any())
        verify(eventSender).sendEvent(any())
    }

    private fun callbackUpdate(): Update {
        val pressedMessage = mock<Message>()
        val pressedMessageChat = chatWithId(100L)
        whenever(pressedMessage.chat()).thenReturn(pressedMessageChat)

        val from = mock<com.pengrad.telegrambot.model.User>()
        whenever(from.languageCode()).thenReturn("en")

        val callbackQuery = mock<CallbackQuery>()
        whenever(callbackQuery.id()).thenReturn("callback-1")
        whenever(callbackQuery.from()).thenReturn(from)
        whenever(callbackQuery.data()).thenReturn("RequestSettingButton")
        whenever(callbackQuery.message()).thenReturn(pressedMessage)

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

    private fun user() = User(
        id = "100",
        language = "en",
        type = Type.FREE_USER,
        lastActivityTimestamp = null
    )
}
