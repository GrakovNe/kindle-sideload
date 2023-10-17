package org.grakovne.sideload.kindle.localization

sealed class Message(val templateName: String)

data class HelpMessage(val items: String) : Message("help_message")

data class HelpMessageItem(
    val key: String,
    val description: String,
    val arguments: List<String> = emptyList()
) : Message("help_message_item")

data object UserConfigurationRequestedMessage: Message("user_configuration_requested")
data object UserConfigurationSubmittedMessage: Message("user_configuration_submitted")

data object FileConvertationRequestedMessage: Message("file_convertation_requested_message")

data class UserConfigurationFailedMessage(
    val reason:  UserConfigurationFailedReason
): Message("user_configuration_failed")
