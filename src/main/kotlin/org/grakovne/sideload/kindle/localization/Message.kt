package org.grakovne.sideload.kindle.localization

sealed class Message(val templateName: String)

data class HelpMessage(val items: String) : Message("help_message")

data class HelpMessageItem(
    val key: String,
    val description: String,
    val arguments: List<String> = emptyList()
) : Message("help_message_item")