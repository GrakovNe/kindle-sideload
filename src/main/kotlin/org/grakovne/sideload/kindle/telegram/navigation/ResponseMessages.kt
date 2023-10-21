package org.grakovne.sideload.kindle.telegram.navigation

import org.grakovne.sideload.kindle.telegram.localization.Message
import org.grakovne.sideload.kindle.telegram.domain.FileUploadFailedReason
import org.grakovne.sideload.kindle.user.configuration.validation.ConfigurationValidationError

data class HelpMessage(val items: String) : Message("help_message")

data class HelpMessageItem(
    val key: String,
    val description: String,
    val arguments: List<String> = emptyList()
) : Message("help_message_item")

data object UserConfigurationRequestedMessage : Message("user_configuration_requested")
data object UserConfigurationRemovedMessage : Message("user_configuration_removed")
data object UserConfigurationSubmittedMessage : Message("user_configuration_submitted")
data object UserConfigurationSubmissionFailedMessage : Message("user_configuration_submission_failed")
data object UserConfigurationFileAbsentMessage : Message("user_configuration_file_absent")
data class UserConfigurationValidationFailedMessage(val reason: ConfigurationValidationError) :
    Message("user_configuration_validation_failed")

data object FileConvertationRequestedMessage : Message("file_convertation_requested_message")

data class FileUploadFailedMessage(
    val reason: FileUploadFailedReason
) : Message("file_upload_failed")

data class FileConvertarionSuccess(val result: String) : Message("file_convertation_success")
data class FileConvertarionFailed(val details: String) : Message("file_convertation_failed")

data object MainScreenRequestedMessage : Message("main_screen_message")