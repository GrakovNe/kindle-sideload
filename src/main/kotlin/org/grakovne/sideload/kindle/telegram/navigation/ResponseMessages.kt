package org.grakovne.sideload.kindle.telegram.navigation

import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.converter.ConvertationError
import org.grakovne.sideload.kindle.telegram.domain.FileUploadFailedReason
import org.grakovne.sideload.kindle.user.configuration.validation.ConfigurationValidationError

data object UserConfigurationRequestedMessage : Message
data object UserConfigurationRemovedMessage : Message
data object UserConfigurationSubmittedMessage : Message
data class UserConfigurationValidationFailedMessage(val reason: ConfigurationValidationError) : Message

data object FileConvertationRequestedMessage : Message

data class FileUploadFailedMessage(val reason: FileUploadFailedReason) : Message

data class FileConvertarionSuccessMessage(val bookShelfUrl: String) : Message
data class FileConvertarionSuccessAutomaticStkMessage(val result: String) : Message
data class FileConvertarionSuccessEmptyOutputMessage(val result: String) : Message

data object FileConvertarionFailedUnsupportedMessage : Message
data class FileConvertarionErrorMessage(val details: String) : Message

data object StkSubmittedMessage : Message
data object StkSuccessMessage : Message
data object StkSuccessAzwMessage : Message
data object StkFailedMessage : Message