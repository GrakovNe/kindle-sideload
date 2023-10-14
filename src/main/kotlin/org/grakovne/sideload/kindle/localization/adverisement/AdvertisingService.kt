package org.grakovne.sideload.kindle.localization.adverisement

import org.grakovne.sideload.kindle.common.domain.Language
import org.grakovne.swiftbot.localization.MessageTemplate
import org.springframework.stereotype.Service

@Service
class AdvertisingService(
    private val advertisementProperties: AdvertisementProperties
) {

    fun provideContent(template: MessageTemplate, language: Language) = advertisementProperties
        .creatives
        .find { it.name == template.advertising.creativeName }
        .takeIf { it?.type == AdvertisingType.ENABLED }
        .takeIf { template.advertising.status == AdvertisingType.ENABLED }
        .let { creative -> creative?.let { advertisementProperties.blockDelimiter + it.text } }
        ?: ""
}