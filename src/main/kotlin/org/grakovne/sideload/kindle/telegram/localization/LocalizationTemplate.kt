package org.grakovne.sideload.kindle.telegram.localization

import org.grakovne.sideload.kindle.telegram.localization.adverisement.AdvertisingType

data class LocalizationTemplate(
    val name: String,
    val type: MessageType,
    val template: String,
    val enablePreview: Boolean = true,
    val advertising: AdvertisingTemplate = AdvertisingTemplate.default
)

data class AdvertisingTemplate(
    val status: AdvertisingType,
    val creativeName: String
) {
    companion object {
        val default = AdvertisingTemplate(AdvertisingType.DISABLED, "")
    }
}

enum class MessageType {
    PLAIN,
    HTML
}