package org.grakovne.sideload.kindle.telegram.navigation

import org.grakovne.sideload.kindle.telegram.domain.FileUploadFailedReason
import org.grakovne.sideload.kindle.telegram.localization.domain.Message
import org.grakovne.sideload.kindle.user.configuration.validation.ConfigurationValidationError

data object UserConfigurationRequestedMessage : Message()
data object UserConfigurationRemovedMessage : Message()
data object UserConfigurationSubmittedMessage : Message()
data object UserConfigurationSubmissionFailedMessage : Message()
data object UserConfigurationFileAbsentMessage : Message()
data class UserConfigurationValidationFailedMessage(val reason: ConfigurationValidationError) : Message()

data object FileConvertationRequestedMessage : Message()

data class FileUploadFailedMessage(val reason: FileUploadFailedReason) : Message()

data class FileConvertarionSuccess(val result: String) : Message()
data class FileConvertarionFailed(val details: String) : Message()

data object MainScreenRequestedMessage : Message()