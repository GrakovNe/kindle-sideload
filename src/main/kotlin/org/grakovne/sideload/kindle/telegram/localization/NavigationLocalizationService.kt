package org.grakovne.sideload.kindle.telegram.localization

import com.fasterxml.jackson.databind.ObjectMapper
import org.grakovne.sideload.kindle.telegram.localization.adverisement.AdvertisingService
import org.springframework.stereotype.Service

@Service
class NavigationLocalizationService(
    objectMapper: ObjectMapper,
    enumLocalizationService: EnumLocalizationService,
    advertisingService: AdvertisingService
) : LocalizationService<NavigationItem>("navigation", enumLocalizationService, advertisingService, objectMapper)