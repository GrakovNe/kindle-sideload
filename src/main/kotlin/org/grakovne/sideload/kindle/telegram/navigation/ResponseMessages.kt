package org.grakovne.sideload.kindle.telegram.navigation

import org.grakovne.sideload.kindle.telegram.domain.FileUploadFailedReason
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.user.configuration.validation.ConfigurationValidationError

data object UserConfigurationRequestedMessage : Message
data object UserConfigurationRemovedMessage : Message
data object UserConfigurationSubmittedMessage : Message
data class UserConfigurationValidationFailedMessage(val reason: ConfigurationValidationError) : Message

data object FileConvertationRequestedMessage : Message

data class FileUploadFailedMessage(val reason: FileUploadFailedReason) : Message

data class FileConvertarionSuccessMessage(val result: String) : Message
data class FileConvertarionFailedMessage(val details: String) : Message

data object StkSuccessMessage : Message
data object StkFailedMessage : Message