package org.grakovne.sideload.kindle.telegram.localization

import org.grakovne.sideload.kindle.telegram.domain.FileUploadFailedReason
import org.grakovne.sideload.kindle.telegram.localization.adverisement.AdvertisingType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EnumLocalizationServiceTest {

    private val sut = EnumLocalizationService(kotlinMapper())

    @Test
    fun `localizes the enum value from the english resource`() {
        assertEquals(
            "Sorry, but your file must be below 20MB",
            sut.localize(FileUploadFailedReason.FILE_IS_TOO_LARGE, "en")
        )
    }

    @Test
    fun `localizes the enum value from the russian resource`() {
        assertEquals(
            "К сожалению, размер файла должен быть меньше 20 мегабайт. Это временные трудности, позже вы сможете загружать файлы любого размера",
            sut.localize(FileUploadFailedReason.FILE_IS_TOO_LARGE, "ru")
        )
    }

    @Test
    fun `falls back to the raw name when the enum is not localized`() {
        assertEquals(
            "ENABLED",
            sut.localize(AdvertisingType.ENABLED, "en")
        )
    }

    @Test
    fun `falls back to the base resource when the language file is absent`() {
        assertEquals(
            "Sorry, but your file must be below 20MB",
            sut.localize(FileUploadFailedReason.FILE_IS_TOO_LARGE, "fr")
        )
    }

    @Test
    fun `falls back to the base resource when the language is unknown`() {
        assertEquals(
            "Sorry, but your file must be below 20MB",
            sut.localize(FileUploadFailedReason.FILE_IS_TOO_LARGE, null)
        )
    }
}
