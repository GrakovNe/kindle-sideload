package org.grakovne.sideload.kindle.telegram.localization.adverisement

import org.grakovne.sideload.kindle.telegram.localization.template.AdvertisingTemplate
import org.grakovne.sideload.kindle.telegram.localization.template.MessageTemplate
import org.grakovne.sideload.kindle.telegram.localization.template.MessageType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AdvertisingServiceTest {

    private fun creative(
        language: String = "en",
        name: String = "promo",
        type: AdvertisingType = AdvertisingType.ENABLED
    ) = AdvertisementCreativeProperties().apply {
        this.language = language
        this.text = "Buy more books!"
        this.name = name
        this.type = type
    }

    private fun properties(
        vararg creatives: AdvertisementCreativeProperties
    ) = AdvertisementProperties().apply {
        blockDelimiter = "\n\n"
        this.creatives = creatives.toList()
    }

    private fun template(status: AdvertisingType = AdvertisingType.ENABLED, creativeName: String = "promo") =
        MessageTemplate(
            name = "Message",
            type = MessageType.HTML,
            template = "Body",
            advertising = AdvertisingTemplate(status, creativeName)
        )

    @Test
    fun `provides the creative text with the block delimiter when everything matches`() {
        val sut = AdvertisingService(properties(creative()))

        assertEquals("\n\nBuy more books!", sut.provideContent(template(), "en"))
    }

    @Test
    fun `returns an empty string when the creative language does not match`() {
        val sut = AdvertisingService(properties(creative(language = "ru")))

        assertEquals("", sut.provideContent(template(), "en"))
    }

    @Test
    fun `returns an empty string when no creative has the requested name`() {
        val sut = AdvertisingService(properties(creative(name = "other")))

        assertEquals("", sut.provideContent(template(), "en"))
    }

    @Test
    fun `returns an empty string when the creative is disabled`() {
        val sut = AdvertisingService(properties(creative(type = AdvertisingType.DISABLED)))

        assertEquals("", sut.provideContent(template(), "en"))
    }

    @Test
    fun `returns an empty string when the template advertising is disabled`() {
        val sut = AdvertisingService(properties(creative()))

        assertEquals("", sut.provideContent(template(status = AdvertisingType.DISABLED), "en"))
    }

    @Test
    fun `returns an empty string when there are no creatives`() {
        val sut = AdvertisingService(properties())

        assertEquals("", sut.provideContent(template(), "en"))
    }

    @Test
    fun `returns an empty string when the language is null and no creative matches`() {
        val sut = AdvertisingService(properties(creative()))

        assertEquals("", sut.provideContent(template(), null))
    }
}
