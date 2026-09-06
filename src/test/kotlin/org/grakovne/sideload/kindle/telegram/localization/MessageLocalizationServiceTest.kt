package org.grakovne.sideload.kindle.telegram.localization

import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.telegram.domain.FileUploadFailedReason
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.MainScreenRequestedMessage
import org.grakovne.sideload.kindle.telegram.localization.adverisement.AdvertisingService
import org.grakovne.sideload.kindle.telegram.navigation.FileConvertarionSuccessMessage
import org.grakovne.sideload.kindle.telegram.navigation.FileUploadFailedMessage
import org.grakovne.sideload.kindle.telegram.navigation.UserConfigurationRequestedMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageLocalizationServiceTest {

    private val advertisingService: AdvertisingService = mock()
    private val sut = MessageLocalizationService(
        kotlinMapper(),
        EnumLocalizationService(kotlinMapper()),
        advertisingService
    )

    @BeforeEach
    fun setUp() {
        // All of the fixtures below use the default (disabled) advertising template,
        // so the real service would append nothing; mirror that instead of Mockito's null.
        whenever(advertisingService.provideContent(any(), any())).thenReturn("")
    }

    @Test
    fun `localizes the parameterless message from the english resource`() {
        val result = sut.localize(UserConfigurationRequestedMessage, "en")

        assertTrue(result.isRight())
        val prepared = result.getOrNull()!!
        assertEquals(
            "Upload a ZIP archive with extended configuration files\n\n" +
                "If you're unsure about the configuration or where to find an example, " +
                "a detailed description can be found in the advanced settings",
            prepared.text
        )
        assertTrue(prepared.enablePreview)
    }

    @Test
    fun `localizes the parameterless message from the russian resource`() {
        val result = sut.localize(UserConfigurationRequestedMessage, "ru")

        assertTrue(result.isRight())
        assertEquals(
            "Загрузите ZIP архив с файлами расширенной конфигурации\n\n" +
                "Если ты не знаешь, что такое конфигурация и где взять пример, " +
                "найти подробное описание можно в расширенных настройках",
            result.getOrNull()!!.text
        )
    }

    @Test
    fun `substitutes the enum value into the template`() {
        val result = sut.localize(FileUploadFailedMessage(FileUploadFailedReason.FILE_IS_TOO_LARGE), "en")

        assertTrue(result.isRight())
        assertEquals("Sorry, but your file must be below 20MB", result.getOrNull()!!.text)
    }

    @Test
    fun `substitutes the message field into the template`() {
        val result = sut.localize(FileConvertarionSuccessMessage(bookShelfUrl = "https://shelf.example.com/abcde"), "ru")

        assertTrue(result.isRight())
        assertEquals(
            "Конвертация прошла успешно\n\n" +
                "Файлы можно отправить их на настроенный E-Mail в течении 24 часов\n\n" +
                "Чтобы скачать файлы напрямую, в браузере Kindle перейдите по адресу " +
                "<a href=\"https://shelf.example.com/abcde\">https://shelf.example.com/abcde</a>",
            result.getOrNull()!!.text
        )
    }

    @Test
    fun `localizes the message with fields from the english resource`() {
        val result = sut.localize(MainScreenRequestedMessage, "en")

        assertTrue(result.isRight())
        assertTrue(result.getOrNull()!!.text.isNotBlank())
    }

    @Test
    fun `reports the template error when there is no template for the message`() {
        val result = sut.localize(UnlocalizedTestMessage(), "en")

        assertTrue(result.isLeft())
        assertEquals(LocalizationError.TEMPLATE_NOT_FOUND, result.swap().getOrNull())
    }

    private class UnlocalizedTestMessage : Message
}
