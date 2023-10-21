package org.grakovne.sideload.kindle.telegram.localization

import arrow.core.Either
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.text.StringSubstitutor
import org.grakovne.sideload.kindle.telegram.domain.PreparedButton
import org.grakovne.sideload.kindle.telegram.localization.domain.Button
import org.grakovne.sideload.kindle.telegram.localization.template.ButtonTemplate
import org.springframework.stereotype.Service

@Service
class NavigationLocalizationService(
    private val objectMapper: ObjectMapper,
    enumLocalizationService: EnumLocalizationService,
) : LocalizationService<Button, PreparedButton, ButtonTemplate>(
    "buttons",
    enumLocalizationService
) {

    override fun applyLocalization(
        language: String?,
        localizedValues: Map<String, String>,
        template: ButtonTemplate
    ) = StringSubstitutor(localizedValues)
        .replace(template.template)
        .let { PreparedButton(it, template.name) }
        .let { Either.Right(it) }

    override fun deserializeResourceContent(content: ByteArray): List<ButtonTemplate> =
        objectMapper.readValue(content, object : TypeReference<List<ButtonTemplate>>() {})
}