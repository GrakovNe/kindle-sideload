package org.grakovne.sideload.kindle.common

import org.grakovne.sideload.kindle.telegram.domain.error.NewEventProcessingError

sealed interface FileUploadFailedError: NewEventProcessingError

data object BookIsTooLargeError: FileUploadFailedError
data object TaskQueueingError: FileUploadFailedError