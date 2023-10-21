package org.grakovne.sideload.kindle.telegram.localization

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.text.StringSubstitutor
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.sideload.kindle.telegram.localization.adverisement.AdvertisingService
import org.springframework.stereotype.Service


@Service
class MessageLocalizationService(
    objectMapper: ObjectMapper,
    enumLocalizationService: EnumLocalizationService,
    advertisingService: AdvertisingService
) : LocalizationService<Message, PreparedMessage>(
    "messages",
    enumLocalizationService,
    advertisingService,
    objectMapper
) {
    override fun applyLocalization(
        language: String?,
        localizedValues: Map<String, String>,
        template: LocalizationTemplate
    ) = StringSubstitutor(localizedValues)
        .replace(template.template)
        .let { it + advertisingService.provideContent(template, language) }
        .let { PreparedMessage(it, template.enablePreview.not()) }
        .let { Either.Right(it) }
}