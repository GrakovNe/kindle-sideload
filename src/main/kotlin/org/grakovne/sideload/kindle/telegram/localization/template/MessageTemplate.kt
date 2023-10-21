package org.grakovne.sideload.kindle.telegram.localization.template

import org.grakovne.sideload.kindle.telegram.localization.adverisement.AdvertisingType

data class MessageTemplate(
    override val name: String,
    val type: MessageType,
    override val template: String,
    val enablePreview: Boolean = true,
    val advertising: AdvertisingTemplate = AdvertisingTemplate.default
) : TextTemplate(name, template)

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