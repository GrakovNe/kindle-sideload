package org.grakovne.sideload.kindle.common

import org.grakovne.sideload.kindle.events.core.EventProcessingError

sealed interface FileUploadFailedError : EventProcessingError

data object BookIsTooLargeError : FileUploadFailedError
data object TaskQueueingError : FileUploadFailedError