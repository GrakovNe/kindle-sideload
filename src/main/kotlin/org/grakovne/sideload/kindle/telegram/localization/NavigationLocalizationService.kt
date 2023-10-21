package org.grakovne.sideload.kindle.telegram.localization

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.text.StringSubstitutor
import org.grakovne.sideload.kindle.telegram.domain.PreparedButton
import org.grakovne.sideload.kindle.telegram.localization.adverisement.AdvertisingService
import org.springframework.stereotype.Service

@Service
class NavigationLocalizationService(
    objectMapper: ObjectMapper,
    enumLocalizationService: EnumLocalizationService,
    advertisingService: AdvertisingService
) : LocalizationService<Button, PreparedButton>("buttons", enumLocalizationService, advertisingService, objectMapper) {

    override fun applyLocalization(
        language: String?,
        localizedValues: Map<String, String>,
        template: LocalizationTemplate
    ) = StringSubstitutor(localizedValues)
        .replace(template.template)
        .let { PreparedButton(it) }
        .let { Either.Right(it) }
}