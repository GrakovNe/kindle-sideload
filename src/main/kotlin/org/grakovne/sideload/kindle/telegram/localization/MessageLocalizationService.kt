package org.grakovne.sideload.kindle.telegram.localization

import com.fasterxml.jackson.databind.ObjectMapper
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.sideload.kindle.telegram.localization.adverisement.AdvertisingService
import org.springframework.stereotype.Service


@Service
class MessageLocalizationService(
    objectMapper: ObjectMapper,
    enumLocalizationService: EnumLocalizationService,
    advertisingService: AdvertisingService
) : LocalizationService<Message>("messages", enumLocalizationService, advertisingService, objectMapper)