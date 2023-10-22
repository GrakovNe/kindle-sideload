package org.grakovne.sideload.kindle.telegram.localization

import arrow.core.Either
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.text.StringSubstitutor
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.sideload.kindle.telegram.localization.adverisement.AdvertisingService
import org.grakovne.sideload.kindle.telegram.localization.domain.Message
import org.grakovne.sideload.kindle.telegram.localization.template.MessageTemplate
import org.springframework.stereotype.Service


@Service
class MessageLocalizationService(
    private val objectMapper: ObjectMapper,
    enumLocalizationService: EnumLocalizationService,
    private val advertisingService: AdvertisingService
) : LocalizationService<Message, PreparedMessage, MessageTemplate>(
    "messages",
    enumLocalizationService
) {
    override fun applyLocalization(
        language: String?,
        localizedValues: Map<String, String>,
        template: MessageTemplate
    ) = StringSubstitutor(localizedValues)
        .replace(template.template)
        .let { it + advertisingService.provideContent(template, language) }
        .let { PreparedMessage(it, template.enablePreview) }
        .let { Either.Right(it) }

    override fun deserializeResourceContent(content: ByteArray): List<MessageTemplate> =
        objectMapper.readValue(content, object : TypeReference<List<MessageTemplate>>() {})
}