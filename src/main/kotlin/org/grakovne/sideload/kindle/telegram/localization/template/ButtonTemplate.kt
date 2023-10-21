package org.grakovne.sideload.kindle.telegram.localization.template

data class ButtonTemplate(
    override val name: String,
    val type: MessageType,
    override val template: String,
) : TextTemplate(name, template)