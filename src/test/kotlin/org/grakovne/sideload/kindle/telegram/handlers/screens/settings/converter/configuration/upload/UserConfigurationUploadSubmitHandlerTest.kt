package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.converter.configuration.upload

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Document
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.GetFile
import com.pengrad.telegrambot.response.GetFileResponse
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.common.FileDownloadService
import org.grakovne.sideload.kindle.common.configuration.FileUploadProperties
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.converter.configuration.UploadConfigurationButton
import org.grakovne.sideload.kindle.telegram.navigation.UserConfigurationSubmittedMessage
import org.grakovne.sideload.kindle.telegram.navigation.UserConfigurationValidationFailedMessage
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.grakovne.sideload.kindle.user.configuration.domain.FileAbsentError
import org.grakovne.sideload.kindle.user.configuration.domain.FileIsTooLargeError
import org.grakovne.sideload.kindle.user.configuration.domain.InternalError
import org.grakovne.sideload.kindle.user.configuration.domain.ValidationError
import org.grakovne.sideload.kindle.user.configuration.validation.ConfigurationValidationError
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseExtractor
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.pengrad.telegrambot.model.File as TelegramFile
import org.grakovne.sideload.kindle.common.navigation.domain.Message as DomainMessage

class UserConfigurationUploadSubmitHandlerTest {

    private val bot: TelegramBot = mock()
    private val restTemplate: RestTemplate = mock()
    private val configurationService: UserConverterConfigurationService = mock()
    private val messageSender: MessageWithNavigationSender = mock()
    private val buttonService: ButtonService = mock()
    private val stateService: UserActivityStateService = mock()
    private val properties = FileUploadProperties().apply { maxSize = 100 }

    // The real [FileDownloadService] drives the production download path against the
    // mocked [RestTemplate]; [FileDownloadService] itself is final and its suspend
    // `download` cannot be stubbed with the mockito-kotlin setup used in this project.
    private val fileDownloadService = FileDownloadService(restTemplate)

    private val handler = UserConfigurationUploadSubmitHandler(
        bot,
        fileDownloadService,
        configurationService,
        properties,
        messageSender,
        buttonService,
        stateService
    )

    private val user = User("user-1", "en", Type.FREE_USER, null)
    private val update: Update = mock()
    private val event = ButtonPressedEvent(update, user)

    private var downloadedFile: File? = null

    @AfterEach
    fun cleanUp() {
        downloadedFile?.delete()
    }

    @Test
    fun `reports the absent file error when the message carries no document`() {
        prepareState()
        whenever(update.message()).thenReturn(null)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Left(FileAbsentError), result)
        verifyNoInteractions(restTemplate)
        verify(configurationService, never()).updateConverterConfiguration(any(), any())
        verify(stateService, never()).setCurrentState(any(), any())
    }

    @Test
    fun `reports the file size error when the document exceeds the limit`() {
        prepareState()
        val document = documentWithSize(101)
        val message = messageWith(document)
        whenever(update.message()).thenReturn(message)

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Left(FileIsTooLargeError), result)
        verifyNoInteractions(restTemplate)
        verify(configurationService, never()).updateConverterConfiguration(any(), any())
    }

    @Test
    fun `reports the internal error when the download yields no file`() {
        bindDownload(file = null)
        prepareState()
        val document = documentWithSize(50)
        val message = messageWith(document)
        val response = getFileResponse("remote-config.zip")
        whenever(update.message()).thenReturn(message)
        whenever(bot.execute(any<GetFile>())).thenReturn(response)
        whenever(bot.getFullFilePath(any<TelegramFile>())).thenReturn("https://cdn/remote-config.zip")

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Left(InternalError), result)
        verify(configurationService, never()).updateConverterConfiguration(any(), any())
        verify(stateService, never()).setCurrentState(any(), any())
    }

    @Test
    fun `downloads the file and updates the converter configuration when the document is valid`() {
        bindDownload(file = "configuration content".toByteArray())
        prepareState()
        val document = documentWithSize(50)
        val message = messageWith(document)
        val response = getFileResponse("remote-config.zip")
        whenever(update.message()).thenReturn(message)
        whenever(bot.execute(any<GetFile>())).thenReturn(response)
        whenever(bot.getFullFilePath(any<TelegramFile>())).thenReturn("https://cdn/remote-config.zip")
        whenever(configurationService.updateConverterConfiguration(eq(user), any<File>()))
            .thenReturn(Either.Right(File("asset.zip")))

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)

        assertNotNull(downloadedFile)
        assertEquals("configuration content", downloadedFile!!.readText())
        verify(configurationService).updateConverterConfiguration(eq(user), any<File>())
        verify(stateService).setCurrentState(eq("user-1"), isNull())
    }

    @Test
    fun `sends the submitted message and resets the activity state when the configuration is accepted`() {
        bindDownload(file = "configuration content".toByteArray())
        val document = documentWithSize(50)
        val message = messageWith(document)
        val response = getFileResponse("remote-config.zip")
        whenever(update.message()).thenReturn(message)
        whenever(bot.execute(any<GetFile>())).thenReturn(response)
        whenever(bot.getFullFilePath(any<TelegramFile>())).thenReturn("https://cdn/remote-config.zip")
        whenever(configurationService.updateConverterConfiguration(eq(user), any<File>()))
            .thenReturn(Either.Right(File("asset.zip")))
        prepareState()

        runBlocking { handler.handleEvent(event) }

        val messageCaptor = argumentCaptor<DomainMessage>()
        verify(messageSender).sendResponse(any<Update>(), eq(user), messageCaptor.capture(), any())
        assertTrue(messageCaptor.firstValue is UserConfigurationSubmittedMessage)
        verify(stateService).setCurrentState(eq("user-1"), isNull())
    }

    @Test
    fun `sends the validation failure message when the configuration is rejected`() {
        bindDownload(file = "configuration content".toByteArray())
        val document = documentWithSize(50)
        val message = messageWith(document)
        val response = getFileResponse("remote-config.zip")
        whenever(update.message()).thenReturn(message)
        whenever(bot.execute(any<GetFile>())).thenReturn(response)
        whenever(bot.getFullFilePath(any<TelegramFile>())).thenReturn("https://cdn/remote-config.zip")
        whenever(configurationService.updateConverterConfiguration(eq(user), any<File>()))
            .thenReturn(Either.Left(ValidationError(ConfigurationValidationError.FILE_IS_NOT_ZIP_FILE)))
        prepareState()

        val result = runBlocking { handler.handleEvent(event) }

        assertEquals(Either.Left(ValidationError(ConfigurationValidationError.FILE_IS_NOT_ZIP_FILE)), result)

        val messageCaptor = argumentCaptor<DomainMessage>()
        verify(messageSender).sendResponse(any<Update>(), eq(user), messageCaptor.capture(), any())
        assertTrue(messageCaptor.firstValue is UserConfigurationValidationFailedMessage)
        verify(stateService, never()).setCurrentState(any(), any())
    }

    /**
     * Stubs [RestTemplate.execute] to route the production extractor of
     * [FileDownloadService.download]: a non-null [file] body is downloaded into a real
     * temporary file, a null body makes `download` return null immediately (no retry
     * delays). The `RequestCallback` is a literal `null` in production, so it is matched
     * with `isNull()`; the trailing vararg is intentionally left unmatched, which makes
     * Mockito accept the (empty) vararg.
     */
    private fun bindDownload(file: ByteArray?) {
        whenever(
            restTemplate.execute(
                any<String>(),
                any<HttpMethod>(),
                isNull(),
                any<ResponseExtractor<*>>()
            )
        ).thenAnswer { invocation ->
            if (file == null) return@thenAnswer null
            @Suppress("UNCHECKED_CAST")
            val extractor = invocation.getArgument(3, ResponseExtractor::class.java)
            val body: ClientHttpResponse = mock()
            whenever(body.body).thenReturn(ByteArrayInputStream(file))
            val result = (extractor as ResponseExtractor<File>).extractData(body)
            downloadedFile = result
            result
        }
    }

    private fun prepareState() {
        whenever(stateService.fetchCurrentState("user-1")).thenReturn("UploadConfigurationButton")
        whenever(buttonService.instance("UploadConfigurationButton")).thenReturn(UploadConfigurationButton)
    }

    private fun messageWith(document: Document): Message {
        val message: Message = mock()
        whenever(message.document()).thenReturn(document)
        return message
    }

    private fun documentWithSize(size: Long): Document {
        val document: Document = mock()
        whenever(document.fileSize()).thenReturn(size)
        whenever(document.fileId()).thenReturn("config-id")
        whenever(document.fileName()).thenReturn("config.zip")
        return document
    }

    private fun getFileResponse(path: String): GetFileResponse {
        val file: TelegramFile = mock()
        whenever(file.fileId()).thenReturn("config-id")
        whenever(file.filePath()).thenReturn(path)
        val response: GetFileResponse = mock()
        whenever(response.file()).thenReturn(file)
        return response
    }
}
