package org.grakovne.swiftbot.localization

import org.grakovne.sideload.kindle.localization.adverisement.AdvertisingType

data class MessageTemplate(
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